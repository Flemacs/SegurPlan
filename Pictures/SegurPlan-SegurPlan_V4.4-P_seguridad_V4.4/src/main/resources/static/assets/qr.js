/* ═══════════════════════════════════════════════════════
   SEGURPLAN — qr.js
   Generador de códigos QR en JavaScript puro (sin librerías ni internet).
   Modo byte (UTF-8), corrección de errores nivel M, versiones 1 a 10
   (hasta ~213 bytes). Uso:
       const svg = SPQr.toSvg('texto');          // cadena <svg>…</svg>
       const matriz = SPQr.encode('texto');      // { size, modules[y][x] }
═══════════════════════════════════════════════════════ */
(function (root) {
    'use strict';

    // Nivel M: códigos de corrección por bloque y número de bloques (versiones 1..10)
    var ECC_PER_BLOCK = [-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26];
    var NUM_BLOCKS    = [-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5];
    var MAX_VERSION = 10;

    function utf8Bytes(text) {
        var s = unescape(encodeURIComponent(text));
        var out = [];
        for (var i = 0; i < s.length; i++) out.push(s.charCodeAt(i));
        return out;
    }

    function rawDataModules(ver) {
        var r = (16 * ver + 128) * ver + 64;
        if (ver >= 2) {
            var n = Math.floor(ver / 7) + 2;
            r -= (25 * n - 10) * n - 55;
            if (ver >= 7) r -= 36;
        }
        return r;
    }

    function dataCodewords(ver) {
        return Math.floor(rawDataModules(ver) / 8) - ECC_PER_BLOCK[ver] * NUM_BLOCKS[ver];
    }

    // ── Reed-Solomon en GF(2^8), polinomio 0x11D ──
    function gfMul(x, y) {
        var z = 0;
        for (var i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }

    function rsDivisor(degree) {
        var result = [];
        for (var i = 0; i < degree - 1; i++) result.push(0);
        result.push(1);
        var rootv = 1;
        for (var i2 = 0; i2 < degree; i2++) {
            for (var j = 0; j < result.length; j++) {
                result[j] = gfMul(result[j], rootv);
                if (j + 1 < result.length) result[j] ^= result[j + 1];
            }
            rootv = gfMul(rootv, 0x02);
        }
        return result;
    }

    function rsRemainder(data, divisor) {
        var result = divisor.map(function () { return 0; });
        data.forEach(function (b) {
            var factor = b ^ result.shift();
            result.push(0);
            divisor.forEach(function (coef, i) {
                result[i] ^= gfMul(coef, factor);
            });
        });
        return result;
    }

    function appendBits(bits, val, len) {
        for (var i = len - 1; i >= 0; i--) bits.push((val >>> i) & 1);
    }

    // ── Datos → palabras código (con relleno) ──
    function buildDataCodewords(bytes, ver) {
        var bits = [];
        appendBits(bits, 0x4, 4);                              // modo byte
        appendBits(bits, bytes.length, ver >= 10 ? 16 : 8);    // nº de caracteres
        bytes.forEach(function (b) { appendBits(bits, b, 8); });

        var capacityBits = dataCodewords(ver) * 8;
        appendBits(bits, 0, Math.min(4, capacityBits - bits.length));
        appendBits(bits, 0, (8 - bits.length % 8) % 8);
        for (var pad = 0xEC; bits.length < capacityBits; pad ^= 0xEC ^ 0x11) {
            appendBits(bits, pad, 8);
        }

        var out = [];
        for (var i = 0; i < bits.length; i += 8) {
            var v = 0;
            for (var k = 0; k < 8; k++) v = (v << 1) | bits[i + k];
            out.push(v);
        }
        return out;
    }

    // ── Bloques + corrección de errores + intercalado ──
    function addEccAndInterleave(data, ver) {
        var numBlocks = NUM_BLOCKS[ver];
        var blockEccLen = ECC_PER_BLOCK[ver];
        var rawCodewords = Math.floor(rawDataModules(ver) / 8);
        var numShort = numBlocks - rawCodewords % numBlocks;
        var shortLen = Math.floor(rawCodewords / numBlocks);

        var blocks = [];
        var divisor = rsDivisor(blockEccLen);
        for (var i = 0, k = 0; i < numBlocks; i++) {
            var dat = data.slice(k, k + shortLen - blockEccLen + (i < numShort ? 0 : 1));
            k += dat.length;
            var ecc = rsRemainder(dat, divisor);
            if (i < numShort) dat.push(0);
            blocks.push(dat.concat(ecc));
        }

        var result = [];
        for (var c = 0; c < blocks[0].length; c++) {
            for (var b = 0; b < blocks.length; b++) {
                if (c !== shortLen - blockEccLen || b >= numShort) result.push(blocks[b][c]);
            }
        }
        return result;
    }

    // ── Matriz ──
    function newMatrix(size) {
        var m = [];
        for (var y = 0; y < size; y++) {
            var row = [];
            for (var x = 0; x < size; x++) row.push(false);
            m.push(row);
        }
        return m;
    }

    function alignmentPositions(ver) {
        if (ver === 1) return [];
        var n = Math.floor(ver / 7) + 2;
        var size = ver * 4 + 17;
        var step = (ver === 32) ? 26 : Math.ceil((ver * 4 + 4) / (n * 2 - 2)) * 2;
        var result = [6];
        for (var pos = size - 7; result.length < n; pos -= step) result.splice(1, 0, pos);
        return result;
    }

    function QrBuilder(ver) {
        this.ver = ver;
        this.size = ver * 4 + 17;
        this.modules = newMatrix(this.size);
        this.isFn = newMatrix(this.size);
    }

    QrBuilder.prototype.setFn = function (x, y, dark) {
        this.modules[y][x] = dark;
        this.isFn[y][x] = true;
    };

    QrBuilder.prototype.drawFunctionPatterns = function () {
        var size = this.size, i;
        for (i = 0; i < size; i++) {
            this.setFn(6, i, i % 2 === 0);
            this.setFn(i, 6, i % 2 === 0);
        }
        this.drawFinder(3, 3);
        this.drawFinder(size - 4, 3);
        this.drawFinder(3, size - 4);

        var pos = alignmentPositions(this.ver), n = pos.length;
        for (i = 0; i < n; i++) {
            for (var j = 0; j < n; j++) {
                if (!((i === 0 && j === 0) || (i === 0 && j === n - 1) || (i === n - 1 && j === 0))) {
                    this.drawAlignment(pos[i], pos[j]);
                }
            }
        }
        this.drawFormatBits(0);
        this.drawVersion();
    };

    QrBuilder.prototype.drawFinder = function (cx, cy) {
        for (var dy = -4; dy <= 4; dy++) {
            for (var dx = -4; dx <= 4; dx++) {
                var dist = Math.max(Math.abs(dx), Math.abs(dy));
                var xx = cx + dx, yy = cy + dy;
                if (xx >= 0 && xx < this.size && yy >= 0 && yy < this.size) {
                    this.setFn(xx, yy, dist !== 2 && dist !== 4);
                }
            }
        }
    };

    QrBuilder.prototype.drawAlignment = function (cx, cy) {
        for (var dy = -2; dy <= 2; dy++) {
            for (var dx = -2; dx <= 2; dx++) {
                this.setFn(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) !== 1);
            }
        }
    };

    QrBuilder.prototype.drawFormatBits = function (mask) {
        var size = this.size, i;
        var data = (0 << 3) | mask;                 // nivel M = 0b00
        var rem = data;
        for (i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        var bits = ((data << 10) | rem) ^ 0x5412;
        function bit(k) { return ((bits >>> k) & 1) !== 0; }

        for (i = 0; i <= 5; i++) this.setFn(8, i, bit(i));
        this.setFn(8, 7, bit(6));
        this.setFn(8, 8, bit(7));
        this.setFn(7, 8, bit(8));
        for (i = 9; i < 15; i++) this.setFn(14 - i, 8, bit(i));

        for (i = 0; i < 8; i++) this.setFn(size - 1 - i, 8, bit(i));
        for (i = 8; i < 15; i++) this.setFn(8, size - 15 + i, bit(i));
        this.setFn(8, size - 8, true);              // módulo oscuro fijo
    };

    QrBuilder.prototype.drawVersion = function () {
        if (this.ver < 7) return;
        var rem = this.ver;
        for (var i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
        var bits = (this.ver << 12) | rem;
        for (var k = 0; k < 18; k++) {
            var bit = ((bits >>> k) & 1) !== 0;
            var a = this.size - 11 + k % 3, b = Math.floor(k / 3);
            this.setFn(a, b, bit);
            this.setFn(b, a, bit);
        }
    };

    QrBuilder.prototype.drawCodewords = function (data) {
        var size = this.size, i = 0;
        for (var right = size - 1; right >= 1; right -= 2) {
            if (right === 6) right = 5;
            for (var vert = 0; vert < size; vert++) {
                for (var j = 0; j < 2; j++) {
                    var x = right - j;
                    var upward = ((right + 1) & 2) === 0;
                    var y = upward ? size - 1 - vert : vert;
                    if (!this.isFn[y][x] && i < data.length * 8) {
                        this.modules[y][x] = ((data[i >>> 3] >>> (7 - (i & 7))) & 1) !== 0;
                        i++;
                    }
                }
            }
        }
    };

    QrBuilder.prototype.applyMask = function (mask) {
        for (var y = 0; y < this.size; y++) {
            for (var x = 0; x < this.size; x++) {
                var invert;
                switch (mask) {
                    case 0: invert = (x + y) % 2 === 0; break;
                    case 1: invert = y % 2 === 0; break;
                    case 2: invert = x % 3 === 0; break;
                    case 3: invert = (x + y) % 3 === 0; break;
                    case 4: invert = (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0; break;
                    case 5: invert = x * y % 2 + x * y % 3 === 0; break;
                    case 6: invert = (x * y % 2 + x * y % 3) % 2 === 0; break;
                    default: invert = ((x + y) % 2 + x * y % 3) % 2 === 0; break;
                }
                if (!this.isFn[y][x] && invert) this.modules[y][x] = !this.modules[y][x];
            }
        }
    };

    // ── Penalización para elegir la mejor máscara ──
    QrBuilder.prototype.finderCount = function (h) {
        var n = h[1];
        var core = n > 0 && h[2] === n && h[3] === n * 3 && h[4] === n && h[5] === n;
        return (core && h[0] >= n * 4 && h[6] >= n ? 1 : 0)
             + (core && h[6] >= n * 4 && h[0] >= n ? 1 : 0);
    };

    QrBuilder.prototype.finderAdd = function (run, h) {
        if (h[0] === 0) run += this.size;
        h.pop();
        h.unshift(run);
    };

    QrBuilder.prototype.finderTerminate = function (color, run, h) {
        if (color) { this.finderAdd(run, h); run = 0; }
        run += this.size;
        this.finderAdd(run, h);
        return this.finderCount(h);
    };

    QrBuilder.prototype.penalty = function () {
        var size = this.size, m = this.modules, result = 0, x, y;

        for (y = 0; y < size; y++) {
            var color = false, run = 0, hist = [0, 0, 0, 0, 0, 0, 0];
            for (x = 0; x < size; x++) {
                if (m[y][x] === color) {
                    run++;
                    if (run === 5) result += 3; else if (run > 5) result++;
                } else {
                    this.finderAdd(run, hist);
                    if (!color) result += this.finderCount(hist) * 40;
                    color = m[y][x];
                    run = 1;
                }
            }
            result += this.finderTerminate(color, run, hist) * 40;
        }

        for (x = 0; x < size; x++) {
            var colorC = false, runC = 0, histC = [0, 0, 0, 0, 0, 0, 0];
            for (y = 0; y < size; y++) {
                if (m[y][x] === colorC) {
                    runC++;
                    if (runC === 5) result += 3; else if (runC > 5) result++;
                } else {
                    this.finderAdd(runC, histC);
                    if (!colorC) result += this.finderCount(histC) * 40;
                    colorC = m[y][x];
                    runC = 1;
                }
            }
            result += this.finderTerminate(colorC, runC, histC) * 40;
        }

        for (y = 0; y < size - 1; y++) {
            for (x = 0; x < size - 1; x++) {
                var c = m[y][x];
                if (c === m[y][x + 1] && c === m[y + 1][x] && c === m[y + 1][x + 1]) result += 3;
            }
        }

        var dark = 0;
        for (y = 0; y < size; y++) for (x = 0; x < size; x++) if (m[y][x]) dark++;
        var total = size * size;
        var k = Math.ceil(Math.abs(dark * 20 - total * 10) / total) - 1;
        result += k * 10;
        return result;
    };

    // ── API pública ──
    function encode(text) {
        var bytes = utf8Bytes(String(text));
        var ver = 0;
        for (var v = 1; v <= MAX_VERSION; v++) {
            var lenBits = v >= 10 ? 16 : 8;
            if (4 + lenBits + bytes.length * 8 <= dataCodewords(v) * 8) { ver = v; break; }
        }
        if (!ver) throw new Error('El texto es demasiado largo para el código QR.');

        var all = addEccAndInterleave(buildDataCodewords(bytes, ver), ver);

        var qr = new QrBuilder(ver);
        qr.drawFunctionPatterns();
        qr.drawCodewords(all);

        var bestMask = 0, bestPenalty = Infinity;
        for (var mask = 0; mask < 8; mask++) {
            qr.applyMask(mask);
            qr.drawFormatBits(mask);
            var p = qr.penalty();
            if (p < bestPenalty) { bestPenalty = p; bestMask = mask; }
            qr.applyMask(mask);                     // deshacer
        }
        qr.applyMask(bestMask);
        qr.drawFormatBits(bestMask);

        return { version: ver, size: qr.size, mask: bestMask, modules: qr.modules };
    }

    function toSvg(text, opts) {
        opts = opts || {};
        var border = opts.border === undefined ? 4 : opts.border;
        var dark = opts.dark || '#000000';
        var light = opts.light || '#ffffff';
        var qr = encode(text);
        var dim = qr.size + border * 2;
        var path = [];
        for (var y = 0; y < qr.size; y++) {
            for (var x = 0; x < qr.size; x++) {
                if (qr.modules[y][x]) path.push('M' + (x + border) + ',' + (y + border) + 'h1v1h-1z');
            }
        }
        return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + dim + ' ' + dim +
               '" width="100%" height="100%" shape-rendering="crispEdges" role="img" aria-label="Código QR de pago">' +
               '<rect width="100%" height="100%" fill="' + light + '"/>' +
               '<path d="' + path.join('') + '" fill="' + dark + '"/></svg>';
    }

    var api = { encode: encode, toSvg: toSvg };
    if (typeof module !== 'undefined' && module.exports) module.exports = api;
    root.SPQr = api;

})(typeof window !== 'undefined' ? window : globalThis);
