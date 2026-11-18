param([string]$BaseUrl="http://host.docker.internal:8080")
$u=[Uri]$BaseUrl
if ($u.Host -notin @("host.docker.internal","localhost","127.0.0.1")) { throw "Destino no autorizado: solo localhost" }
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw "Docker no disponible" }
New-Item -ItemType Directory -Force seguridad/evidencias | Out-Null
$path=(Resolve-Path seguridad/evidencias).Path
# Baseline: escaneo pasivo; no ejecuta active scan
 docker run --rm -v "${path}:/zap/wrk:rw" --add-host=host.docker.internal:host-gateway ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t $BaseUrl -r zap-baseline.html -J zap-baseline.json -I
