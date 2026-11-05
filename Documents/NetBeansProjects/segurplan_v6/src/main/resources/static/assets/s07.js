/* SEGURPLAN — Pantalla 7: Simulación AFP */
function simularAFP(e) {
  e.preventDefault();
  const edad   = parseInt(document.getElementById('sim-edad').value)   || 65;
  const anios  = parseInt(document.getElementById('sim-anios').value)  || 20;
  const fondo  = parseFloat(document.getElementById('sim-fondo').value)  || 120000;
  const aporte = parseFloat(document.getElementById('sim-aporte').value) || 500;

  /* Estimación simple: fondo acumulado * factor edad + aportes futuros */
  const factorEdad = edad >= 65 ? 0.016 : 0.013;
  const monto = (fondo * factorEdad) + (aporte * 0.02 * (65 - Math.min(edad, 64)));
  const montoFmt = Math.max(800, monto).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  sessionStorage.setItem('afp_monto', 'S/ ' + montoFmt);
  sessionStorage.setItem('afp_fondo', 'S/ ' + fondo.toLocaleString('es-PE', { minimumFractionDigits: 2 }));
  sessionStorage.setItem('afp_anios', anios + ' años');

  window.location.href = 's08-resultado-afp.html';
}
