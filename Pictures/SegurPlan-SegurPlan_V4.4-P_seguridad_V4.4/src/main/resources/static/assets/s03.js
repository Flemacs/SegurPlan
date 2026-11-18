/* SEGURPLAN — Pantalla 3: Solicitud de cotización */
const ASEGURADORAS = {
  'Rímac Seguros':      ['Seguro vehicular','SOAT','Seguro de vida','Seguro de salud','Seguro de hogar','Seguro de accidentes personales','Seguro de viaje','Seguro de desgravamen'],
  'Pacífico Seguros':   ['Seguro vehicular','SOAT','Seguro de vida','Seguro de salud','Seguro oncológico','Seguro de hogar','Seguro de accidentes personales','Seguro de viaje'],
  'La Positiva':        ['Seguro vehicular','SOAT','Seguro de vida','Seguro de salud','Seguro de hogar','Seguro agrícola','Seguro de accidentes personales','Seguro estudiantil'],
  'Mapfre Perú':        ['Seguro vehicular','SOAT','Seguro de vida','Seguro de hogar','Seguro de accidentes personales','Seguro de viaje','Seguro de desgravamen'],
  'Interseguro':        ['Seguro de vida','Seguro de vida con ahorro','Seguro de desgravamen','Seguro oncológico','Seguro de accidentes personales','Seguro de sepelio'],
  'Seguros SURA':       ['Seguro vehicular','SOAT','Seguro de vida','Seguro de salud','Seguro de hogar','Seguro de accidentes personales','Seguro de viaje'],
  'BNP Paribas Cardif': ['Seguro de desgravamen','Seguro de vida','Seguro de accidentes personales','Seguro de desempleo','Seguro oncológico'],
};

const CAMPOS = {
  'Seguro vehicular':          [['Placa del vehículo','text'],['Marca','text'],['Modelo','text']],
  'SOAT':                      [['Placa del vehículo','text'],['Marca','text'],['Modelo','text']],
  'Seguro de vida':            [['Fecha de nacimiento','date'],['Suma asegurada (S/)','number']],
  'Seguro de vida con ahorro': [['Fecha de nacimiento','date'],['Suma asegurada (S/)','number']],
  'Seguro de salud':           [['Fecha de nacimiento','date'],['N° de beneficiarios','number']],
  'Seguro de hogar':           [['Dirección del inmueble','text'],['Tipo de inmueble','text']],
  'Seguro oncológico':         [['Fecha de nacimiento','date'],['Observaciones','text']],
  'Seguro de accidentes personales': [['Fecha de nacimiento','date'],['Observaciones','text']],
  'Seguro estudiantil':        [['Fecha de nacimiento','date'],['Centro educativo','text']],
  'Seguro de sepelio':         [['Fecha de nacimiento','date'],['Observaciones','text']],
  'Seguro de desempleo':       [['Fecha de nacimiento','date'],['Observaciones','text']],
  'Seguro de desgravamen':     [['Entidad financiera','text'],['Monto del crédito (S/)','number']],
  'Seguro de viaje':           [['Destino','text'],['Fecha de salida','date'],['Fecha de retorno','date']],
  'Seguro agrícola':           [['Tipo de cultivo','text'],['Hectáreas','number']],
};

function onEmpresaChange() {
  const empresa = document.getElementById('sel-empresa').value;
  const selTipo = document.getElementById('sel-tipo');
  const panel   = document.getElementById('seguros-panel');
  const ul      = document.getElementById('seguros-ul');
  const spTitle = document.getElementById('sp-title');

  selTipo.innerHTML = '<option value="">-- Seleccionar tipo --</option>';
  selTipo.disabled = !empresa;
  document.getElementById('datos-req').style.display = 'none';

  if (!empresa) { panel.style.display = 'none'; return; }

  const seguros = ASEGURADORAS[empresa] || [];
  seguros.forEach(s => {
    const opt = document.createElement('option');
    opt.value = s; opt.textContent = s;
    selTipo.appendChild(opt);
  });

  spTitle.textContent = 'Seguros disponibles en ' + empresa;
  ul.innerHTML = '';
  seguros.forEach(s => {
    const li = document.createElement('li');
    li.textContent = s; li.dataset.val = s;
    ul.appendChild(li);
  });
  panel.style.display = 'block';
}

function onTipoChange() {
  const tipo = document.getElementById('sel-tipo').value;
  document.getElementById('seguros-ul').querySelectorAll('li').forEach(li => {
    li.className = li.dataset.val === tipo ? 'active' : '';
  });

  const reqDiv = document.getElementById('datos-req');
  const title  = document.getElementById('req-title');
  const campos = document.getElementById('req-campos');

  if (!tipo) { reqDiv.style.display = 'none'; return; }

  title.textContent = 'Datos requeridos — ' + tipo;
  campos.innerHTML = '';
  (CAMPOS[tipo] || [['Información adicional','text']]).forEach(([lbl, type]) => {
    const g = document.createElement('div');
    g.className = 'form-group';
    g.innerHTML = `<label>${lbl}</label><input type="${type}" class="form-control" />`;
    campos.appendChild(g);
  });
  reqDiv.style.display = 'block';
}
