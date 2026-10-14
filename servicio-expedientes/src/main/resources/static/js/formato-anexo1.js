/**
 * formato-anexo1.js
 * Lógica interactiva para el Anexo N° 1: FORMATO DE DECLARACIÓN JURADA PARA LICENCIA DE FUNCIONAMIENTO
 * Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 163-2020-PCM
 * Municipalidad Provincial de Huamanga
 */

const API_EXPEDIENTES = '/api/expedientes';
const API_FORMULARIOS = '/api/formularios';

// Estado global de la aplicación
let estadoFormato = {
  expedienteActual: null,   // Objeto ExpedienteResponseDto si fue guardado o cargado
  modalidadSeleccionada: 'LICENCIA_INDETERMINADA',
  nivelRiesgoSeleccionado: 'BAJO',
  canvasCroquis: null,
  ctxCroquis: null,
  dibujando: false,
};

// =============================================================================
// INICIALIZACIÓN
// =============================================================================
document.addEventListener('DOMContentLoaded', () => {
  inicializarFechas();
  inicializarCanvasCroquis();
  configurarSincronizacionEncabezados();
  configurarEventosCambio();

  // Si en la URL viene ?tramite=EXP-XXXX o ?id=UUID, cargarlo automáticamente
  const params = new URLSearchParams(window.location.search);
  const tramiteParam = params.get('tramite');
  const idParam = params.get('id');

  if (tramiteParam) {
    document.getElementById('inputBuscarExpediente').value = tramiteParam;
    buscarYPrecargarExpediente(tramiteParam);
  } else if (idParam) {
    cargarExpedientePorId(idParam);
  } else {
    // Por defecto cargar croquis ilustrativo inicial
    cargarCroquisEjemplo();
  }
});

function inicializarFechas() {
  const hoy = new Date();
  const fechaStr = formatearFecha(hoy);

  const elFechaRecep = document.getElementById('hdrFechaRecepcion');
  const elFechaFirma = document.getElementById('txtFechaFirma');
  const elFechaPago  = document.getElementById('hdrFechaPago');

  if (elFechaRecep && !elFechaRecep.value) elFechaRecep.value = fechaStr;
  if (elFechaFirma && !elFechaFirma.value) elFechaFirma.value = fechaStr;
  if (elFechaPago && !elFechaPago.value)   elFechaPago.value = fechaStr;

  sincronizarEncabezados();
}

function formatearFecha(d) {
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const yyyy = d.getFullYear();
  return `${dd}/${mm}/${yyyy}`;
}

// =============================================================================
// SINCRONIZACIÓN DE ENCABEZADOS Y FIRMAS
// =============================================================================
function configurarSincronizacionEncabezados() {
  const camposSync = [
    { p1: 'hdrNumeroExpediente', p2: 'hdrNumeroExpediente2' },
    { p1: 'hdrFechaRecepcion',   p2: 'hdrFechaRecepcion2' },
    { p1: 'hdrReciboPago',       p2: 'hdrReciboPago2' },
    { p1: 'hdrFechaPago',        p2: 'hdrFechaPago2' },
  ];

  camposSync.forEach(c => {
    const el1 = document.getElementById(c.p1);
    const el2 = document.getElementById(c.p2);
    if (el1 && el2) {
      el1.addEventListener('input', () => {
        el2.value = el1.value;
      });
    }
  });
}

function sincronizarEncabezados() {
  const campos = ['hdrNumeroExpediente', 'hdrFechaRecepcion', 'hdrReciboPago', 'hdrFechaPago'];
  campos.forEach(id => {
    const v1 = document.getElementById(id);
    const v2 = document.getElementById(id + '2');
    if (v1 && v2) v2.value = v1.value;
  });
}

function sincronizarFirmante() {
  const titular = document.getElementById('txtTitularNombre').value.trim();
  const dniTitular = document.getElementById('txtDocumentoIdentidad').value.trim();
  const repNombre = document.getElementById('txtRepNombre').value.trim();
  const repDni = document.getElementById('txtRepDni').value.trim();

  const elFirmaNombres = document.getElementById('txtFirmaNombres');
  const elFirmaDni = document.getElementById('txtFirmaDni');

  if (repNombre) {
    elFirmaNombres.value = repNombre.toUpperCase();
    elFirmaDni.value = repDni || dniTitular;
  } else {
    elFirmaNombres.value = titular.toUpperCase();
    elFirmaDni.value = dniTitular;
  }
}

function configurarEventosCambio() {
  const titularEl = document.getElementById('txtTitularNombre');
  const docEl = document.getElementById('txtDocumentoIdentidad');
  if (titularEl) titularEl.addEventListener('input', sincronizarFirmante);
  if (docEl) docEl.addEventListener('input', sincronizarFirmante);
}

// =============================================================================
// SELECCIÓN DE MODALIDAD DEL TRÁMITE
// =============================================================================
function alCambiarModalidad(modalidad) {
  estadoFormato.modalidadSeleccionada = modalidad;
  const checkboxes = document.querySelectorAll('input[name="modTramite"]');
  checkboxes.forEach(chk => {
    if (chk.id !== obtenerIdCheckboxModalidad(modalidad)) {
      chk.checked = false;
    }
  });
}

function obtenerIdCheckboxModalidad(mod) {
  switch (mod) {
    case 'LICENCIA_INDETERMINADA':     return 'modIndeterminada';
    case 'LICENCIA_TEMPORAL':          return 'modTemporal';
    case 'LICENCIA_CON_ANUNCIO':       return 'modConAnuncio';
    case 'LICENCIA_CESIONARIO':        return 'modCesionario';
    case 'LICENCIA_MERCADOS_GALERIAS': return 'modMercados';
    case 'CAMBIO_DENOMINACION':        return 'modCambioDenom';
    case 'TRANSFERENCIA_LICENCIA':     return 'modTransferencia';
    case 'CESE_ACTIVIDADES':           return 'modCese';
    case 'OTROS':                      return 'modOtros';
    default:                           return 'modIndeterminada';
  }
}

function marcarModalidadEnUi(mod) {
  estadoFormato.modalidadSeleccionada = mod;
  const checkboxes = document.querySelectorAll('input[name="modTramite"]');
  checkboxes.forEach(c => c.checked = false);
  const targetId = obtenerIdCheckboxModalidad(mod);
  const target = document.getElementById(targetId);
  if (target) target.checked = true;
}

// =============================================================================
// CÁLCULO Y ESTIMACIÓN DE RIESGO SEGÚN ÁREA (m²)
// =============================================================================
function alCambiarArea() {
  const val = parseFloat(document.getElementById('txtAreaMetros').value);
  const hintEl = document.getElementById('txtAreaHint');

  if (!val || isNaN(val) || val <= 0) {
    hintEl.innerHTML = 'Clasificación ITSE estimada: <em>Ingrese área</em>';
    return;
  }

  if (val < 100) {
    seleccionarNivelRiesgo('BAJO');
    hintEl.innerHTML = 'Clasificación ITSE estimada: <strong style="color: #059669;">Riesgo BAJO (Ex Post)</strong>';
  } else if (val < 500) {
    seleccionarNivelRiesgo('MEDIO');
    hintEl.innerHTML = 'Clasificación ITSE estimada: <strong style="color: #d97706;">Riesgo MEDIO (Ex Post)</strong>';
  } else if (val < 1000) {
    seleccionarNivelRiesgo('ALTO');
    hintEl.innerHTML = 'Clasificación ITSE estimada: <strong style="color: #dc2626;">Riesgo ALTO (ITSE Previa requerida)</strong>';
  } else {
    seleccionarNivelRiesgo('MUY_ALTO');
    hintEl.innerHTML = 'Clasificación ITSE estimada: <strong style="color: #991b1b;">Riesgo MUY ALTO (ITSE Previa multidisciplinaria)</strong>';
  }
}

function seleccionarNivelRiesgo(nivel) {
  estadoFormato.nivelRiesgoSeleccionado = nivel;
  ['chkRiesgoBajo', 'chkRiesgoMedio', 'chkRiesgoAlto', 'chkRiesgoMuyAlto'].forEach(id => {
    document.getElementById(id).checked = false;
  });

  switch (nivel) {
    case 'BAJO':     document.getElementById('chkRiesgoBajo').checked = true; break;
    case 'MEDIO':    document.getElementById('chkRiesgoMedio').checked = true; break;
    case 'ALTO':     document.getElementById('chkRiesgoAlto').checked = true; break;
    case 'MUY_ALTO': document.getElementById('chkRiesgoMuyAlto').checked = true; break;
  }
}

function toggleAutorizacionSectorial() {
  const chk = document.getElementById('chkRequiereSectorial');
  const row = document.getElementById('rowSectorial');
  if (chk.checked) {
    row.style.opacity = '1';
    document.getElementById('txtSectorEntidad').focus();
  } else {
    row.style.opacity = '0.7';
  }
}

function irAInstrucciones() {
  const el = document.getElementById('seccionInstrucciones');
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' });
    el.style.outline = '2px solid #0284c7';
    setTimeout(() => { el.style.outline = 'none'; }, 2000);
  }
}

// =============================================================================
// CANVAS INTERACTIVO DE CROQUIS DE UBICACIÓN
// =============================================================================
function inicializarCanvasCroquis() {
  const canvas = document.getElementById('canvasCroquis');
  if (!canvas) return;

  estadoFormato.canvasCroquis = canvas;
  const ctx = canvas.getContext('2d');
  estadoFormato.ctxCroquis = ctx;

  // Eventos de ratón
  canvas.addEventListener('mousedown', iniciarDibujo);
  canvas.addEventListener('mousemove', trazarDibujo);
  canvas.addEventListener('mouseup', detenerDibujo);
  canvas.addEventListener('mouseleave', detenerDibujo);

  // Eventos táctiles
  canvas.addEventListener('touchstart', (e) => {
    e.preventDefault();
    const touch = e.touches[0];
    const rect = canvas.getBoundingClientRect();
    iniciarDibujo({ clientX: touch.clientX, clientY: touch.clientY, rect });
  });

  canvas.addEventListener('touchmove', (e) => {
    e.preventDefault();
    const touch = e.touches[0];
    trazarDibujo({ clientX: touch.clientX, clientY: touch.clientY });
  });

  canvas.addEventListener('touchend', detenerDibujo);
}

function getCanvasPos(e) {
  const canvas = estadoFormato.canvasCroquis;
  const rect = canvas.getBoundingClientRect();
  const scaleX = canvas.width / rect.width;
  const scaleY = canvas.height / rect.height;
  return {
    x: (e.clientX - rect.left) * scaleX,
    y: (e.clientY - rect.top) * scaleY,
  };
}

function iniciarDibujo(e) {
  estadoFormato.dibujando = true;
  const ctx = estadoFormato.ctxCroquis;
  const pos = getCanvasPos(e);
  ctx.beginPath();
  ctx.moveTo(pos.x, pos.y);
  ctx.strokeStyle = '#002b5c';
  ctx.lineWidth = 2;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

  const overlay = document.getElementById('croquisOverlayText');
  if (overlay) overlay.style.display = 'none';
}

function trazarDibujo(e) {
  if (!estadoFormato.dibujando) return;
  const ctx = estadoFormato.ctxCroquis;
  const pos = getCanvasPos(e);
  ctx.lineTo(pos.x, pos.y);
  ctx.stroke();
}

function detenerDibujo() {
  estadoFormato.dibujando = false;
}

function limpiarCanvasCroquis() {
  const canvas = estadoFormato.canvasCroquis;
  const ctx = estadoFormato.ctxCroquis;
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  const overlay = document.getElementById('croquisOverlayText');
  if (overlay) overlay.style.display = 'block';
}

function cargarCroquisEjemplo() {
  const canvas = estadoFormato.canvasCroquis;
  if (!canvas) return;
  const ctx = estadoFormato.ctxCroquis;
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Fondo tenue de cuadrícula
  ctx.save();
  ctx.strokeStyle = '#e2e8f0';
  ctx.lineWidth = 0.8;
  for (let x = 0; x < canvas.width; x += 25) {
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
  }
  for (let y = 0; y < canvas.height; y += 25) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
  }

  // Trazar manzanas y vías típicas del Centro Histórico de Huamanga
  ctx.strokeStyle = '#334155';
  ctx.lineWidth = 1.5;

  // Calle principal: Jr. 28 de Julio (horizontal)
  ctx.fillStyle = '#f8fafc';
  ctx.fillRect(40, 50, 480, 50);
  ctx.strokeRect(40, 50, 480, 50);

  // Calle transversal: Jr. Callao (vertical)
  ctx.fillRect(240, 10, 60, 130);
  ctx.strokeRect(240, 10, 60, 130);

  // Manzana / Lote del establecimiento
  ctx.fillStyle = '#fef08a';
  ctx.strokeStyle = '#b45309';
  ctx.lineWidth = 2;
  ctx.fillRect(150, 15, 80, 35);
  ctx.strokeRect(150, 15, 80, 35);

  // Marcador de puerta y flecha
  ctx.fillStyle = '#dc2626';
  ctx.beginPath();
  ctx.arc(190, 48, 4, 0, Math.PI * 2);
  ctx.fill();

  // Nombres de calles y rótulos
  ctx.fillStyle = '#0f172a';
  ctx.font = 'bold 9px sans-serif';
  ctx.fillText('JR. 28 DE JULIO (CUADRA 3)', 60, 80);
  ctx.fillText('JR. CALLAO', 246, 135);

  ctx.fillStyle = '#b45309';
  ctx.font = 'bold 8.5px sans-serif';
  ctx.fillText('★ LOCAL (N° 245)', 155, 36);

  // Rosa de los vientos / Norte
  ctx.strokeStyle = '#003366';
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.moveTo(520, 35);
  ctx.lineTo(520, 15);
  ctx.stroke();
  ctx.fillStyle = '#003366';
  ctx.beginPath();
  ctx.moveTo(520, 10);
  ctx.lineTo(516, 18);
  ctx.lineTo(524, 18);
  ctx.closePath();
  ctx.fill();
  ctx.font = 'bold 8px sans-serif';
  ctx.fillText('N', 517, 9);

  ctx.restore();

  const overlay = document.getElementById('croquisOverlayText');
  if (overlay) overlay.style.display = 'none';
}

function subirImagenCroquis(event) {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(e) {
    const img = new Image();
    img.onload = function() {
      const canvas = estadoFormato.canvasCroquis;
      const ctx = estadoFormato.ctxCroquis;
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      const overlay = document.getElementById('croquisOverlayText');
      if (overlay) overlay.style.display = 'none';
      mostrarToast('📷 Croquis cargado correctamente desde imagen', 'success');
    };
    img.src = e.target.result;
  };
  reader.readAsDataURL(file);
}

// =============================================================================
// CARGAR DATOS DEMO (MYPES HUAMANGA)
// =============================================================================
function cargarDatosDemo() {
  // Sección I: Modalidad
  marcarModalidadEnUi('LICENCIA_INDETERMINADA');
  document.getElementById('txtPlazoTemporal').value = '';
  document.getElementById('txtTipoAnuncio').value = '';
  document.getElementById('txtLicenciaCesionario').value = '';

  // Sección II: Solicitante
  document.getElementById('txtTitularNombre').value = 'MENDOZA PALOMINO MARIA ELENA';
  document.getElementById('txtDocumentoIdentidad').value = '42158963';
  document.getElementById('txtRuc').value = '10421589631';
  document.getElementById('txtTelefono').value = '966123456';
  document.getElementById('txtCorreoElectronico').value = 'elena.mendoza@munihuamanga.gob.pe';
  document.getElementById('txtSolVia').value = 'Jr. 28 de Julio';
  document.getElementById('txtSolNroInt').value = 'N° 245';
  document.getElementById('txtSolUrb').value = 'Centro Histórico';
  document.getElementById('txtSolDistProv').value = 'Ayacucho - Huamanga';

  // Sección III: Representante (opcional)
  document.getElementById('txtRepNombre').value = '';
  document.getElementById('txtRepDni').value = '';
  document.getElementById('txtRepSunarp').value = '';

  // Sección IV: Establecimiento
  document.getElementById('txtNombreComercial').value = 'CAFETERÍA Y ARTESANÍAS EL HUAMANGUINO';
  document.getElementById('txtCiiuCodigo').value = 'I5630';
  document.getElementById('txtGiroNegocio').value = 'Cafetería, expendio de postres tradicionales y artesanía ayacuchana';
  document.getElementById('txtActividadDetallada').value = 'Venta de café orgánico, repostería típica y souvenirs artesanales';
  document.getElementById('txtZonificacion').value = 'ZRE-CH (Zona Reglamentación Especial Centro Histórico)';
  document.getElementById('txtEstVia').value = 'Jr. 28 de Julio';
  document.getElementById('txtEstNroInt').value = 'N° 245 Int. A';
  document.getElementById('txtEstUrb').value = 'Centro Histórico';
  document.getElementById('txtEstProvincia').value = 'Huamanga';

  // Sectorial (no requiere)
  document.getElementById('chkRequiereSectorial').checked = false;
  document.getElementById('txtSectorEntidad').value = '';
  document.getElementById('txtSectorDenominacion').value = '';
  document.getElementById('txtSectorFecha').value = '';
  document.getElementById('txtSectorNumero').value = '';

  // Área
  document.getElementById('txtAreaMetros').value = '65.50';
  alCambiarArea();

  // Croquis
  cargarCroquisEjemplo();

  // Sección V: Declaraciones Juradas
  document.getElementById('chkJuradaPoder').checked = false;
  document.getElementById('chkJuradaSeguridad').checked = true;
  document.getElementById('chkJuradaTituloSalud').checked = false;
  document.getElementById('txtObservaciones').value = 'Establecimiento ubicado en zona monumental, cuenta con pozo a tierra vigente y extintor PQS de 6kg conforme a la normativa ITSE.';
  
  const hoyStr = formatearFecha(new Date());
  document.getElementById('txtFechaFirma').value = hoyStr;
  document.getElementById('hdrFechaRecepcion').value = hoyStr;
  document.getElementById('hdrReciboPago').value = 'VCH-SAT-2026-0814';
  document.getElementById('hdrFechaPago').value = hoyStr;

  sincronizarEncabezados();
  sincronizarFirmante();

  mostrarToast('🪄 Datos de demostración de Huamanga cargados con éxito', 'info');
}

function limpiarFormato() {
  if (!confirm('¿Desea limpiar todos los campos del formato?')) return;

  const inputs = document.querySelectorAll('.formato-container input, .formato-container textarea');
  inputs.forEach(input => {
    if (input.type === 'checkbox') {
      input.checked = false;
    } else {
      input.value = '';
    }
  });

  marcarModalidadEnUi('LICENCIA_INDETERMINADA');
  document.getElementById('chkJuradaSeguridad').checked = true;
  seleccionarNivelRiesgo('BAJO');
  document.getElementById('txtSolDistProv').value = 'Ayacucho - Huamanga';
  document.getElementById('txtEstProvincia').value = 'Huamanga';
  document.getElementById('txtCalificadorNombre').value = 'Ing. Calificador ITSE — Reg. CENEPRED N° 04821';

  inicializarFechas();
  sincronizarFirmante();
  limpiarCanvasCroquis();

  estadoFormato.expedienteActual = null;
  const badge = document.getElementById('badgeEstadoDoc');
  badge.className = 'status-pill status-draft';
  badge.textContent = '📝 Borrador sin guardar';

  mostrarToast('Formulario restablecido a borrador en blanco', 'info');
}

// =============================================================================
// GUARDAR EN BASE DE DATOS (POST /api/expedientes)
// =============================================================================
async function guardarEnBaseDeDatos() {
  const btn = document.getElementById('btnGuardarDb');
  const titular     = valor('txtTitularNombre');
  const documento   = valor('txtDocumentoIdentidad');
  const telefono    = valor('txtTelefono');
  const correo      = valor('txtCorreoElectronico');
  const nomCom      = valor('txtNombreComercial');
  const giro        = valor('txtGiroNegocio');
  const areaMetros  = parseFloat(valor('txtAreaMetros'));

  // Validaciones obligatorias
  if (!titular)   return alerta('El campo "Apellidos y Nombres / Razón social" es obligatorio en la Sección II.');
  if (!documento) return alerta('El campo "N° DNI / N° C.E." es obligatorio.');
  if (!telefono)  return alerta('El número de teléfono o celular es obligatorio.');
  if (!correo)    return alerta('El correo electrónico de contacto es obligatorio.');
  if (!nomCom)    return alerta('El "Nombre comercial" del negocio es obligatorio en la Sección IV.');
  if (!giro)      return alerta('El "Giro" o actividad comercial es obligatorio en la Sección IV.');
  if (!areaMetros || isNaN(areaMetros) || areaMetros <= 0) {
    return alerta('El "Área total solicitada (m²)" debe ser un valor numérico mayor a 0.');
  }

  // Determinar Tipo de Persona y Tipo Documento
  const esJuridica = (documento.length === 11 && (documento.startsWith('20') || documento.startsWith('10')));
  const tipoPersona = esJuridica ? 'JURIDICA' : 'NATURAL';
  const tipoDoc = documento.length === 11 ? 'RUC' : (documento.length === 8 ? 'DNI' : 'CARNET_EXTRANJERIA');

  // Dirección desglosada
  const estVia = valor('txtEstVia') || valor('txtSolVia') || 'Jr. 28 de Julio';
  const estNro = valor('txtEstNroInt') || 'S/N';
  const estUrb = valor('txtEstUrb') || 'Centro Histórico';
  const direccionCompleta = `${estVia} ${estNro}, ${estUrb}, Huamanga`;

  // Construir payload CrearExpedienteDto compatible con el backend
  const payload = {
    solicitanteId: crypto.randomUUID ? crypto.randomUUID() : 'f47ac10b-58cc-4372-a567-0e02b2c3d479',
    modalidadTramite: estadoFormato.modalidadSeleccionada || 'LICENCIA_INDETERMINADA',
    plazoTemporalMeses: parseInt(valor('txtPlazoTemporal')) || null,
    tipoAnuncio: valor('txtTipoAnuncio') || null,
    numeroLicenciaPrincipal: valor('txtLicenciaCesionario') || valor('txtLicenciaCambio') || valor('txtLicenciaTransferencia') || valor('txtLicenciaCese') || null,
    
    tipoPersona: tipoPersona,
    tipoDocumento: tipoDoc,
    nombreTitular: titular,
    documentoIdentidad: documento,
    razonSocial: esJuridica ? titular : (valor('txtRuc') ? titular : null),
    telefono: telefono,
    correoElectronico: correo,
    autorizaNotificacion: true,

    partidaSunarp: valor('txtRepSunarp') || null,
    dniRepresentante: valor('txtRepDni') || null,
    nombreRepresentante: valor('txtRepNombre') || null,

    nombreComercial: nomCom,
    ciiuCodigo: valor('txtCiiuCodigo') || 'I5630',
    giroNegocio: giro,
    actividadDetallada: valor('txtActividadDetallada') || null,
    zonificacion: valor('txtZonificacion') || 'ZRE-CH',
    direccionEstablecimiento: direccionCompleta,
    tipoVia: estVia.split(' ')[0] || 'Jr.',
    nombreVia: estVia.substring(estVia.indexOf(' ') + 1) || estVia,
    numeroVivienda: estNro,
    interior: valor('txtEstNroInt') || null,
    urbanizacion: estUrb,
    distrito: 'Ayacucho',
    provincia: valor('txtEstProvincia') || 'Huamanga',
    departamento: 'Ayacucho',

    areaMetrosCuadrados: areaMetros,
    requiereAutorizacionSectorial: document.getElementById('chkRequiereSectorial').checked,
    sectorEntidad: valor('txtSectorEntidad') || null,
    sectorDenominacion: valor('txtSectorDenominacion') || null,
    sectorFecha: valor('txtSectorFecha') || null,
    sectorNumero: valor('txtSectorNumero') || null,

    motivoObservacion: valor('txtObservaciones') || null,
  };

  btn.disabled = true;
  btn.innerHTML = '⏳ Guardando en BD...';

  try {
    const res = await fetch(API_EXPEDIENTES, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Error del servidor HTTP ${res.status}`);
    }

    const exp = await res.json();
    estadoFormato.expedienteActual = exp;

    // Actualizar encabezado del formato oficial
    document.getElementById('hdrNumeroExpediente').value = exp.numeroTramite;
    document.getElementById('hdrNumeroExpediente2').value = exp.numeroTramite;
    if (exp.fechaCreacion) {
      const f = formatearFecha(new Date(exp.fechaCreacion));
      document.getElementById('hdrFechaRecepcion').value = f;
      document.getElementById('hdrFechaRecepcion2').value = f;
    }

    // Actualizar Pill de Estado
    const badge = document.getElementById('badgeEstadoDoc');
    badge.className = 'status-pill status-saved';
    badge.innerHTML = `✅ Guardado en BD: <strong>${exp.numeroTramite}</strong>`;

    mostrarToast(`🎉 Expediente ${exp.numeroTramite} registrado y almacenado con éxito en la base de datos municipal. Ya puede descargar el PDF oficial.`, 'success');

  } catch (err) {
    alert('❌ Error al guardar en base de datos: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.innerHTML = '💾 Guardar en Base de Datos';
  }
}

// =============================================================================
// DESCARGA EN PDF OFICIAL (Ley N° 28976 / D.S. N° 046-2017-PCM)
// =============================================================================
async function descargarPdfOficial() {
  const btn = document.getElementById('btnDescargarPdf');
  btn.disabled = true;
  btn.innerHTML = '⏳ Generando PDF...';

  try {
    // Si ya fue guardado en base de datos y tiene ID UUID
    if (estadoFormato.expedienteActual && estadoFormato.expedienteActual.id) {
      const id = estadoFormato.expedienteActual.id;
      const url = `${API_FORMULARIOS}/anexo-1/${id}/pdf`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`Error HTTP ${res.status}`);
      const blob = await res.blob();
      descargarBlob(blob, `Anexo1-DeclaracionJurada-${estadoFormato.expedienteActual.numeroTramite}.pdf`);
      mostrarToast('📥 Descarga de PDF oficial completada', 'success');
      return;
    }

    // Si aún no está guardado, generar PDF bajo demanda enviando el DTO completo
    const dto = construirDtoParaPdf();
    const res = await fetch(`${API_FORMULARIOS}/anexo1-declaracion-jurada`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dto),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Error al generar PDF: HTTP ${res.status}`);
    }

    const blob = await res.blob();
    const numTramite = dto.numeroTramite || 'BORRADOR';
    descargarBlob(blob, `Anexo1-DeclaracionJurada-${numTramite}.pdf`);
    mostrarToast('📥 Descarga de PDF oficial completada', 'success');

  } catch (err) {
    alert('❌ No se pudo descargar el PDF oficial: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.innerHTML = '⬇️ Descargar PDF Oficial';
  }
}

function construirDtoParaPdf() {
  const titular = valor('txtTitularNombre') || 'SOLICITANTE';
  const doc = valor('txtDocumentoIdentidad') || '00000000';
  const estVia = valor('txtEstVia') || valor('txtSolVia') || 'Jr. 28 de Julio N° 245';
  const numTramite = valor('hdrNumeroExpediente') || ('EXP-' + new Date().getFullYear() + '-00001');

  return {
    id: estadoFormato.expedienteActual ? estadoFormato.expedienteActual.id : null,
    numeroTramite: numTramite,
    modalidadTramite: estadoFormato.modalidadSeleccionada || 'LICENCIA_INDETERMINADA',
    plazoTemporalMeses: parseInt(valor('txtPlazoTemporal')) || null,
    tipoAnuncio: valor('txtTipoAnuncio') || null,
    numeroLicenciaPrincipal: valor('txtLicenciaCesionario') || null,

    tipoPersona: doc.length === 11 ? 'JURIDICA' : 'NATURAL',
    tipoDocumento: doc.length === 11 ? 'RUC' : 'DNI',
    nombreTitular: titular,
    documentoIdentidad: doc,
    razonSocial: valor('txtRuc') ? titular : null,
    telefono: valor('txtTelefono'),
    correoElectronico: valor('txtCorreoElectronico'),

    partidaSunarp: valor('txtRepSunarp'),
    dniRepresentante: valor('txtRepDni'),
    nombreRepresentante: valor('txtRepNombre'),

    nombreComercial: valor('txtNombreComercial') || titular,
    ciiuCodigo: valor('txtCiiuCodigo') || 'I5630',
    giroNegocio: valor('txtGiroNegocio') || 'Comercio menor',
    actividadDetallada: valor('txtActividadDetallada'),
    zonificacion: valor('txtZonificacion') || 'ZRE-CH',
    direccionEstablecimiento: estVia,
    tipoVia: estVia.split(' ')[0] || 'Jr.',
    nombreVia: estVia,
    numeroVivienda: valor('txtEstNroInt'),
    urbanizacion: valor('txtEstUrb') || 'Huamanga',
    distrito: 'Ayacucho',
    provincia: valor('txtEstProvincia') || 'Huamanga',

    areaMetrosCuadrados: parseFloat(valor('txtAreaMetros')) || 45.0,
    nivelRiesgo: estadoFormato.nivelRiesgoSeleccionado || 'BAJO',
    requiereAutorizacionSectorial: document.getElementById('chkRequiereSectorial').checked,
    sectorEntidad: valor('txtSectorEntidad'),
    sectorDenominacion: valor('txtSectorDenominacion'),
    sectorFecha: valor('txtSectorFecha'),
    sectorNumero: valor('txtSectorNumero'),

    numeroOperacionSat: valor('hdrReciboPago') || null,
    motivoObservacion: valor('txtObservaciones') || null,
  };
}

function descargarBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.style.display = 'none';
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(url);
  document.body.removeChild(a);
}

// =============================================================================
// IMPRESIÓN DEL FORMATO
// =============================================================================
function imprimirFormato() {
  sincronizarFirmante();
  sincronizarEncabezados();
  window.print();
}

// =============================================================================
// BÚSQUEDA Y PRECARGA DE EXPEDIENTE EXISTENTE
// =============================================================================
async function buscarYPrecargarExpediente(numParam) {
  const tramite = numParam || valor('inputBuscarExpediente');
  if (!tramite) return alerta('Ingrese un número de trámite (ej: EXP-2026-00001).');

  try {
    // Consultar endpoint público de detalle de expediente para formatos
    let res = await fetch(`${API_EXPEDIENTES}/detalle/${encodeURIComponent(tramite)}`);
    let exp = null;

    if (res.ok) {
      exp = await res.json();
    } else {
      // Fallback a listado general si estuviera autenticado
      res = await fetch(`${API_EXPEDIENTES}`);
      if (res.ok) {
        const lista = await res.json();
        exp = lista.find(e => e.numeroTramite.toUpperCase() === tramite.toUpperCase());
      }
    }

    if (!exp) {
      throw new Error(`No se encontró el expediente "${tramite}" en la base de datos.`);
    }

    cargarDatosEnFormato(exp);
    mostrarToast(`📂 Expediente ${exp.numeroTramite} cargado exitosamente`, 'success');

  } catch (err) {
    alert('⚠️ ' + err.message);
  }
}

async function cargarExpedientePorId(id) {
  try {
    const res = await fetch(`${API_EXPEDIENTES}/${id}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const exp = await res.json();
    cargarDatosEnFormato(exp);
    mostrarToast(`📂 Expediente ${exp.numeroTramite} cargado exitosamente`, 'success');
  } catch (err) {
    console.error('Error al cargar por ID', err);
  }
}

function cargarDatosEnFormato(exp) {
  estadoFormato.expedienteActual = exp;

  // Header
  document.getElementById('hdrNumeroExpediente').value = exp.numeroTramite || '';
  document.getElementById('hdrNumeroExpediente2').value = exp.numeroTramite || '';
  if (exp.fechaCreacion) {
    const f = formatearFecha(new Date(exp.fechaCreacion));
    document.getElementById('hdrFechaRecepcion').value = f;
    document.getElementById('hdrFechaRecepcion2').value = f;
  }
  if (exp.numeroOperacionSat || exp.voucherId) {
    const r = exp.numeroOperacionSat || exp.voucherId;
    document.getElementById('hdrReciboPago').value = r;
    document.getElementById('hdrReciboPago2').value = r;
  }
  if (exp.fechaPagoSat) {
    const fp = formatearFecha(new Date(exp.fechaPagoSat));
    document.getElementById('hdrFechaPago').value = fp;
    document.getElementById('hdrFechaPago2').value = fp;
  }

  // Modalidad
  if (exp.modalidadTramite) {
    marcarModalidadEnUi(exp.modalidadTramite);
  }
  if (exp.plazoTemporalMeses) document.getElementById('txtPlazoTemporal').value = exp.plazoTemporalMeses + ' meses';
  if (exp.tipoAnuncio) document.getElementById('txtTipoAnuncio').value = exp.tipoAnuncio;
  if (exp.numeroLicenciaPrincipal) {
    document.getElementById('txtLicenciaCesionario').value = exp.numeroLicenciaPrincipal;
    document.getElementById('txtLicenciaCambio').value = exp.numeroLicenciaPrincipal;
    document.getElementById('txtLicenciaTransferencia').value = exp.numeroLicenciaPrincipal;
    document.getElementById('txtLicenciaCese').value = exp.numeroLicenciaPrincipal;
  }

  // Solicitante
  document.getElementById('txtTitularNombre').value = exp.nombreTitular || exp.razonSocial || '';
  document.getElementById('txtDocumentoIdentidad').value = exp.documentoIdentidad || '';
  document.getElementById('txtRuc').value = (exp.documentoIdentidad && exp.documentoIdentidad.length === 11) ? exp.documentoIdentidad : '';
  document.getElementById('txtTelefono').value = exp.telefono || '';
  document.getElementById('txtCorreoElectronico').value = exp.correoElectronico || '';

  const viaStr = exp.tipoVia ? `${exp.tipoVia} ${exp.nombreVia || ''}` : (exp.direccionEstablecimiento || '');
  document.getElementById('txtSolVia').value = viaStr;
  document.getElementById('txtSolNroInt').value = exp.numeroVivienda || exp.interior || '';
  document.getElementById('txtSolUrb').value = exp.urbanizacion || '';
  document.getElementById('txtSolDistProv').value = `${exp.distrito || 'Ayacucho'} - ${exp.provincia || 'Huamanga'}`;

  // Representante
  document.getElementById('txtRepNombre').value = exp.nombreRepresentante || '';
  document.getElementById('txtRepDni').value = exp.dniRepresentante || '';
  document.getElementById('txtRepSunarp').value = exp.partidaSunarp ? `${exp.partidaSunarp} ${exp.asientoSunarp ? '- ' + exp.asientoSunarp : ''}` : '';

  // Establecimiento
  document.getElementById('txtNombreComercial').value = exp.nombreComercial || '';
  document.getElementById('txtCiiuCodigo').value = exp.ciiuCodigo || '';
  document.getElementById('txtGiroNegocio').value = exp.giroNegocio || '';
  document.getElementById('txtActividadDetallada').value = exp.actividadDetallada || '';
  document.getElementById('txtZonificacion').value = exp.zonificacion || '';

  document.getElementById('txtEstVia').value = viaStr;
  document.getElementById('txtEstNroInt').value = exp.numeroVivienda || exp.interior || '';
  document.getElementById('txtEstUrb').value = exp.urbanizacion || '';
  document.getElementById('txtEstProvincia').value = exp.provincia || 'Huamanga';

  // Sectorial
  if (exp.requiereAutorizacionSectorial) {
    document.getElementById('chkRequiereSectorial').checked = true;
    document.getElementById('txtSectorEntidad').value = exp.sectorEntidad || '';
    document.getElementById('txtSectorDenominacion').value = exp.sectorDenominacion || '';
    document.getElementById('txtSectorFecha').value = exp.sectorFecha || '';
    document.getElementById('txtSectorNumero').value = exp.sectorNumero || '';
  }

  // Área
  if (exp.areaMetrosCuadrados) {
    document.getElementById('txtAreaMetros').value = exp.areaMetrosCuadrados;
    alCambiarArea();
  }

  // Nivel de Riesgo
  if (exp.nivelRiesgo) {
    seleccionarNivelRiesgo(exp.nivelRiesgo);
  }

  // Observaciones
  if (exp.motivoObservacion) {
    document.getElementById('txtObservaciones').value = exp.motivoObservacion;
  }

  sincronizarFirmante();

  // Status
  const badge = document.getElementById('badgeEstadoDoc');
  badge.className = 'status-pill status-saved';
  badge.innerHTML = `✅ Registrado en BD: <strong>${exp.numeroTramite}</strong> (${exp.estado})`;
}

// =============================================================================
// HELPERS
// =============================================================================
function valor(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : '';
}

function alerta(msg) {
  alert('⚠️ ' + msg);
  return false;
}

function mostrarToast(msg, tipo = 'info') {
  const toast = document.getElementById('toastNotification');
  const icon  = document.getElementById('toastIcon');
  const msgEl = document.getElementById('toastMessage');

  if (!toast) return;

  icon.textContent = tipo === 'success' ? '✅' : (tipo === 'error' ? '❌' : 'ℹ️');
  msgEl.textContent = msg;

  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 4000);
}
