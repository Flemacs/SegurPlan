/* SEGURPLAN — Pantalla 9: Registro de siniestro (pasos) */
let currentStep = 1;
const TOTAL_STEPS = 4;

function goStep(n) {
  if (n < 1 || n > TOTAL_STEPS) return;
  document.querySelectorAll('.step-tab').forEach((t, i) => {
    t.classList.toggle('active', i + 1 === n);
    t.classList.toggle('done',   i + 1 < n);
  });
  document.querySelectorAll('.step-panel').forEach((p, i) => {
    p.classList.toggle('active', i + 1 === n);
  });
  currentStep = n;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function nextStep() { goStep(currentStep + 1); }
function prevStep() { goStep(currentStep - 1); }
