/**
 * portal-ciudadano.js — Fase 3
 * Wizard multipaso con integración a endpoints REST de expedientes y formularios.
 * Ley N° 28976 / D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM
 */

const API_BASE        = '/api/expedientes';
const API_FORMULARIOS = '/api/formularios';  // Endpoints integrados en el Monolito Modular

// ─── Estado global del wizard ─────────────────────────────────────────────────
let wizardState = {
  pasoActual: 1,
  totalPasos: 3,
  expedienteRegistrado: null,   // ExpedienteResponseDto recibido tras el POST
};

// ─── Progreso (%) por paso ────────────────────────────────────────────────────
const PROGRESO = { 1: 16.67, 2: 50, 3: 100 };

// =============================================================================
// NAVEGACIÓN ENTRE PESTAÑAS PRINCIPALES
// =============================================================================
function mostrarPestana(tab) {
  const secRegistro    = document.getElementById('secRegistro');
  const secSeguimiento = document.getElementById('secSeguimiento');
  const btnRegistro    = document.getElementById('tabBtnRegistro');
  const btnSeguimiento = document.getElementById('tabBtnSeguimiento');

  if (tab === 'registro') {
    secRegistro.style.display    = 'block';
    secSeguimiento.style.display = 'none';
    btnRegistro.className        = 'btn btn-primary';
    btnSeguimiento.className     = 'btn btn-secondary';
  } else {
    secRegistro.style.display    = 'none';
    secSeguimiento.style.display = 'block';
    btnRegistro.className        = 'btn btn-secondary';
    btnSeguimiento.className     = 'btn btn-primary';
    consultarTramite();
  }
}

// =============================================================================
// NAVEGACIÓN ENTRE PASOS DEL WIZARD
// =============================================================================
function irPaso(numeroPaso) {
  const pasoActual = wizardState.pasoActual;

  // Validar paso actual antes de avanzar
  if (numeroPaso > pasoActual) {
    if (!validarPaso(pasoActual)) return;
  }

  // Ocultar panel actual
  const panelActual = document.getElementById(`wizPanel${pasoActual}`);
  panelActual.classList.remove('active');

  // Mostrar nuevo panel
  const panelNuevo = document.getElementById(`wizPanel${numeroPaso}`);
  panelNuevo.style.animation = numeroPaso > pasoActual
    ? 'slideInRight 0.35s ease-out'
    : 'slideInLeft 0.35s ease-out';
  panelNuevo.classList.add('active');

  // Si va al paso 3, construir resumen
  if (numeroPaso === 3) {
    construirResumen();
  }

  // Actualizar stepper
  actualizarStepper(pasoActual, numeroPaso);

  // Actualizar barra de progreso
  document.getElementById('wizProgressFill').style.width = PROGRESO[numeroPaso] + '%';

  wizardState.pasoActual = numeroPaso;
}

function actualizarStepper(pasoAnterior, pasoNuevo) {
  for (let i = 1; i <= wizardState.totalPasos; i++) {
    const stepEl = document.getElementById(`wizStep${i}`);
    stepEl.classList.remove('active', 'completed');
    if (i < pasoNuevo) {
      stepEl.classList.add('completed');
      stepEl.querySelector('.wizard-step-node').textContent = '✓';
    } else if (i === pasoNuevo) {
      stepEl.classList.add('active');
      stepEl.querySelector('.wizard-step-node').textContent = i;
    } else {
      stepEl.querySelector('.wizard-step-node').textContent = i;
    }
  }
}

// =============================================================================
// VALIDACIÓN POR PASO
// =============================================================================
function validarPaso(paso) {
  if (paso === 1) {
    const nombre = valor('nombreTitular');
    const doc    = valor('documentoIdentidad');
    const tel    = valor('telefono');
    const email  = valor('correoElectronico');

    if (!nombre) return mostrarError('El nombre del titular es obligatorio.');
    const regexDoc = /^([0-9]{8}|(10|20)[0-9]{9})$/;
    if (!regexDoc.test(doc)) return mostrarError('El documento debe ser un DNI (8 dígitos) o RUC (11 dígitos, iniciado en 10 o 20).');
    const regexTel = /^9[0-9]{8}$/;
    if (!regexTel.test(tel)) return mostrarError('Ingrese un número de celular válido (9XXXXXXXX).');
    const regexEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!regexEmail.test(email)) return mostrarError('Ingrese un correo electrónico válido.');
  }

  if (paso === 2) {
    const nombre   = valor('nombreComercial');
    const giro     = valor('giroNegocio');
    const dir      = valor('direccionEstablecimiento');
    const area     = parseFloat(document.getElementById('areaMetrosCuadrados').value);

    if (!nombre) return mostrarError('El nombre comercial del negocio es obligatorio.');
    if (!giro)   return mostrarError('El giro o actividad económica es obligatorio.');
    if (!dir)    return mostrarError('La dirección del establecimiento es obligatoria.');
    if (!area || area <= 0) return mostrarError('El área en m² debe ser un número mayor a 0.');
  }

  return true;
}

function valor(id) {
  return document.getElementById(id).value.trim();
}

function mostrarError(msg) {
  alert('⚠️ ' + msg);
  return false;
}

// =============================================================================
// RESUMEN EN PASO 3
// =============================================================================
function construirResumen() {
  const area = parseFloat(document.getElementById('areaMetrosCuadrados').value);
  const nivelRiesgoEstimado = area >= 500 ? '🔴 ALTO / MUY ALTO (ITSE Previa requerida)' : '🟡 BAJO / MEDIO (ITSE Posterior)';

  const items = [
    { label: 'Titular / Solicitante',    valor: valor('nombreTitular') },
    { label: 'Documento de Identidad',   valor: valor('documentoIdentidad') },
    { label: 'Razón Social',             valor: valor('razonSocial') || 'Persona Natural' },
    { label: 'Teléfono',                 valor: valor('telefono') },
    { label: 'Correo Electrónico',       valor: valor('correoElectronico') },
    { label: 'Nombre Comercial',         valor: valor('nombreComercial') },
    { label: 'Actividad Económica',      valor: valor('giroNegocio') },
    { label: 'Dirección del Local',      valor: valor('direccionEstablecimiento') },
    { label: 'Área (m²)',               valor: area + ' m²' },
    { label: 'Riesgo ITSE Estimado',     valor: nivelRiesgoEstimado },
  ];

  document.getElementById('resumenDatos').innerHTML = items.map(it => `
    <div class="resumen-item">
      <strong>${it.label}</strong>
      <span>${it.valor || '-'}</span>
    </div>
  `).join('');
}

// =============================================================================
// REGISTRO DE SOLICITUD (POST /api/expedientes)
// =============================================================================
async function registrarSolicitud() {
  const chk = document.getElementById('chkVeracidad');
  if (!chk.checked) {
    return alert('⚠️ Debe aceptar la Declaración Jurada de Veracidad para continuar.');
  }

  const btn = document.getElementById('btnEnviar');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Enviando solicitud...';

  const payload = {
    solicitanteId: crypto.randomUUID ? crypto.randomUUID() : 'f47ac10b-58cc-4372-a567-0e02b2c3d479',
    nombreTitular:           valor('nombreTitular'),
    documentoIdentidad:      valor('documentoIdentidad'),
    razonSocial:             valor('razonSocial') || null,
    nombreComercial:         valor('nombreComercial'),
    giroNegocio:             valor('giroNegocio'),
    direccionEstablecimiento: valor('direccionEstablecimiento'),
    areaMetrosCuadrados:     parseFloat(document.getElementById('areaMetrosCuadrados').value),
    telefono:                valor('telefono'),
    correoElectronico:       valor('correoElectronico'),
  };

  try {
    const res = await fetch(API_BASE, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Error HTTP ${res.status}`);
    }

    const exp = await res.json();
    wizardState.expedienteRegistrado = exp;

    // Mostrar banner de éxito y formatos
    mostrarBannerExito(exp);

  } catch (err) {
    alert('❌ Error al registrar solicitud: ' + err.message);
    btn.disabled = false;
    btn.innerHTML = '🚀 Enviar Solicitud a Mesa de Partes Digitales';
  }
}

function mostrarBannerExito(exp) {
  // Ocultar formulario de envío
  document.getElementById('zonaBtnEnviar').style.display  = 'none';
  document.getElementById('wizNavPaso3').style.display     = 'none';
  document.getElementById('resumenDatos').style.display    = 'none';
  document.getElementById('chkVeracidad').parentElement.parentElement.style.display = 'none';

  // Mostrar banner
  document.getElementById('bannerExito').style.display = 'block';
  document.getElementById('txtNumeroTramiteGenerado').textContent = exp.numeroTramite;

  // Precargar el campo de seguimiento para ir directo
  const inputSeg = document.getElementById('inputBuscarTramite');
  if (inputSeg) inputSeg.value = exp.numeroTramite;

  // Barra de progreso al 100%
  document.getElementById('wizProgressFill').style.width = '100%';
}

// =============================================================================
// DESCARGA DE FORMATOS OFICIALES (POST /api/formularios/...)
// =============================================================================
async function descargarAnexo(tipoFormato) {
  const exp = wizardState.expedienteRegistrado;
  if (!exp) return alert('No hay expediente registrado.');

  const btnId = {
    'declaracion-jurada':        'btnAnexo1',
    'anexo3-matriz-riesgo-itse': 'btnAnexo3',
    'anexo4-condiciones-seguridad': 'btnAnexo4',
  }[tipoFormato];

  const btn = document.getElementById(btnId);
  const textoOriginal = btn ? btn.innerHTML : '';
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Generando PDF...';
  }

  try {
    // En el Monolito Modular, todos los documentos se descargan directamente desde /api/expedientes/{id}/documentos/...
    let url;
    if (tipoFormato === 'declaracion-jurada') {
      url = `${API_BASE}/${exp.id}/documentos/anexo1-declaracion-jurada`;
    } else if (tipoFormato === 'anexo3-matriz-riesgo-itse') {
      url = `${API_BASE}/${exp.id}/documentos/anexo3-matriz-riesgo-itse`;
    } else if (tipoFormato === 'anexo4-condiciones-seguridad') {
      url = `${API_BASE}/${exp.id}/documentos/anexo4-condiciones-seguridad`;
    }

    let blob;
    if (url) {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`Error al generar PDF: HTTP ${res.status}`);
      blob = await res.blob();
    } else {
      // Fallback al endpoint POST integrado en el monolito si fuera necesario
      const endpoint = tipoFormato === 'anexo3-matriz-riesgo-itse'
        ? `${API_FORMULARIOS}/anexo3-matriz-riesgo-itse`
        : `${API_FORMULARIOS}/anexo4-condiciones-seguridad`;
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(exp),
      });
      if (!res.ok) throw new Error(`Error al generar PDF: HTTP ${res.status}`);
      blob = await res.blob();
    }

    // Disparar descarga en el navegador
    const objUrl   = URL.createObjectURL(blob);
    const link     = document.createElement('a');
    link.href      = objUrl;
    const nombreArchivo = tipoFormato === 'declaracion-jurada'
      ? `Anexo1-DeclaracionJurada-${exp.numeroTramite}.pdf`
      : tipoFormato === 'anexo3-matriz-riesgo-itse'
        ? `Anexo3-MatrizRiesgoITSE-${exp.numeroTramite}.pdf`
        : `Anexo4-CondicionesSeguridad-${exp.numeroTramite}.pdf`;
    link.download  = nombreArchivo;
    link.click();
    URL.revokeObjectURL(objUrl);

    // Marcar como generado
    if (btn) {
      btn.innerHTML = '✅ Descargado';
      btn.style.opacity = '0.75';
    }

  } catch (err) {
    alert('❌ Error al descargar el PDF: ' + err.message);
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = textoOriginal;
    }
  }
}

// =============================================================================
// MESA DE AYUDA / GUÍA DE LLENADO DE ANEXOS 3 Y 4 (FASE PREPARATORIA)
// =============================================================================
function abrirMesaAyuda(tipoAnexo) {
  const modal = document.getElementById('modalMesaAyuda');
  const titulo = document.getElementById('mesaAyudaTitulo');
  const contenido = document.getElementById('mesaAyudaContenido');
  if (!modal) return;

  if (tipoAnexo === 'anexo3') {
    titulo.innerHTML = '🛡️ Mesa de Ayuda: Llenado y Trámite del Anexo 3 (Matriz de Riesgo ITSE)';
    contenido.innerHTML = `
      <div style="background: #fef3c7; border-left: 4px solid #f59e0b; padding: 1rem; border-radius: 6px; margin-bottom: 1rem;">
        <strong style="color: #92400e;">⚠️ Instrucción Importante para el Administrado:</strong>
        <p style="margin: 0.5rem 0 0 0; color: #78350f; font-size: 0.88rem;">
          Este documento ha sido generado automáticamente con los datos de su actividad y local comercial. Debe <strong>descargarlo, imprimirlo y llevarlo físicamente</strong> ante la <strong>Subgerencia de Defensa Civil (Gestión de Riesgo de Desastres)</strong> de la MPH para su revisión y firma oficial por el Inspector Técnico acreditado.
        </p>
      </div>
      <h4 style="margin-top: 1rem; color: #1e293b;">📋 Pasos para su Tramitación:</h4>
      <ol style="margin-left: 1.25rem; font-size: 0.88rem; color: #334155; line-height: 1.6;">
        <li><strong>Imprima 2 copias</strong> del Anexo 3 generado en tamaño A4.</li>
        <li>Acérquese a las oficinas de <strong>Defensa Civil de la MPH</strong> (Jr. Bellido N° 123).</li>
        <li>El Inspector Acreditado CENEPRED verificará los factores agravantes (aforo, almacenamiento de GLP, pisos) y suscribirá el dictamen de inspección.</li>
      </ol>
      <div style="background: #e0f2fe; border-left: 4px solid #0284c7; padding: 0.75rem; border-radius: 6px; margin-top: 1rem;">
        <span style="color: #0369a1; font-size: 0.85rem;">
          💡 <strong>Próximamente (Etapa Final):</strong> Se habilitará un visor de ejemplo interactivo con ayuda contextual campo a campo y validación asistida en línea.
        </span>
      </div>
    `;
  } else if (tipoAnexo === 'anexo4') {
    titulo.innerHTML = '🔒 Mesa de Ayuda: Llenado y Trámite del Anexo 4 (Condiciones de Seguridad)';
    contenido.innerHTML = `
      <div style="background: #ecfdf5; border-left: 4px solid #10b981; padding: 1rem; border-radius: 6px; margin-bottom: 1rem;">
        <strong style="color: #065f46;">⚠️ Instrucción Importante para el Administrado:</strong>
        <p style="margin: 0.5rem 0 0 0; color: #047857; font-size: 0.88rem;">
          El Anexo 4 es una Declaración Jurada de 4 páginas sobre las condiciones de seguridad en edificaciones (extintores, señalética, tablero eléctrico, pozo a tierra). Debe <strong>imprimirlo, firmarlo personalmente y llevarlo</strong> a la Subgerencia correspondiente según el nivel de riesgo.
        </p>
      </div>
      <h4 style="margin-top: 1rem; color: #1e293b;">📋 Recomendaciones Clave de Llenado y Firma:</h4>
      <ul style="margin-left: 1.25rem; font-size: 0.88rem; color: #334155; line-height: 1.6;">
        <li><strong>Firma del Administrado:</strong> Suscribir en la última hoja con firma manuscrita y huella digital (o firma del representante legal si es persona jurídica).</li>
        <li><strong>Firma Técnica (si aplica):</strong> Para locales de riesgo Alto o Muy Alto, adjuntar el protocolo de pruebas de pozo a tierra firmado por un Ingeniero Electricista o Mecánico Electricista colegiado.</li>
        <li><strong>Presentación:</strong> Presentar conjuntamente con el cargo del expediente en mesa de partes / ventanilla de licencias.</li>
      </ul>
      <div style="background: #e0f2fe; border-left: 4px solid #0284c7; padding: 0.75rem; border-radius: 6px; margin-top: 1rem;">
        <span style="color: #0369a1; font-size: 0.85rem;">
          💡 <strong>Próximamente (Etapa Final):</strong> Se integrará la ventana interactiva de ejemplo con modelos resueltos de llenado según su tipo de establecimiento comercial.
        </span>
      </div>
    `;
  } else {
    titulo.innerHTML = 'ℹ️ Mesa de Ayuda de Trámites y Formatos';
    contenido.innerHTML = `
      <p style="font-size: 0.88rem; color: #334155;">
        Para asistencia personalizada sobre el llenado de formatos, puede consultar a la Plataforma de Atención al Ciudadano de la Municipalidad Provincial de Huamanga o al correo <em>licencias@munihuamanga.gob.pe</em>.
      </p>
    `;
  }

  modal.style.display = 'flex';
}

function cerrarMesaAyuda() {
  const modal = document.getElementById('modalMesaAyuda');
  if (modal) modal.style.display = 'none';
}

// =============================================================================
// ACCIONES DE NAVEGACIÓN POST-REGISTRO
// =============================================================================
function irASeguimiento() {
  mostrarPestana('seguimiento');
}

function nuevaSolicitud() {
  // Reset completo del wizard
  wizardState.expedienteRegistrado = null;
  wizardState.pasoActual = 1;

  // Limpiar formulario
  ['nombreTitular','documentoIdentidad','razonSocial','telefono','correoElectronico',
   'nombreComercial','giroNegocio','direccionEstablecimiento','areaMetrosCuadrados'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  document.getElementById('chkVeracidad').checked = false;
  document.getElementById('selNumPisos').selectedIndex = 0;

  // Ocultar banner, mostrar formularios
  document.getElementById('bannerExito').style.display = 'none';
  document.getElementById('zonaBtnEnviar').style.display = 'block';
  document.getElementById('wizNavPaso3').style.display = 'flex';
  document.getElementById('chkVeracidad').parentElement.parentElement.style.display = 'block';

  const btnEnviar = document.getElementById('btnEnviar');
  btnEnviar.disabled = false;
  btnEnviar.innerHTML = '🚀 Enviar Solicitud a Mesa de Partes Digitales';

  // Volver al paso 1
  document.getElementById(`wizPanel${wizardState.pasoActual}`).classList.remove('active');
  document.getElementById('wizPanel1').classList.add('active');
  for (let i = 1; i <= 3; i++) {
    const stepEl = document.getElementById(`wizStep${i}`);
    stepEl.classList.remove('active', 'completed');
    stepEl.querySelector('.wizard-step-node').textContent = i;
    if (i === 1) stepEl.classList.add('active');
  }
  document.getElementById('wizProgressFill').style.width = PROGRESO[1] + '%';
  wizardState.pasoActual = 1;
}

// =============================================================================
// CONSULTA DE TRÁMITE (PESTAÑA SEGUIMIENTO)
// =============================================================================
async function consultarTramite() {
  const numero = document.getElementById('inputBuscarTramite').value.trim();
  if (!numero) return;

  try {
    const res = await fetch(`${API_BASE}/tramite/${numero}`);
    if (!res.ok) throw new Error('Expediente no encontrado. Verifique el número ingresado.');
    const exp = await res.json();

    document.getElementById('resultadoConsulta').style.display = 'block';

    // Rellenar ficha
    document.getElementById('resNumeroTramite').innerText = exp.numeroTramite;
    document.getElementById('resTitular').innerText = exp.nombreTitular + (exp.razonSocial ? ` (${exp.razonSocial})` : '');
    document.getElementById('resGiro').innerText = exp.giroNegocio;
    document.getElementById('resDireccion').innerText = exp.direccionEstablecimiento + ` (${exp.areaMetrosCuadrados} m²)`;
    document.getElementById('resNivelRiesgo').innerText = exp.nivelRiesgo ? `${exp.nivelRiesgo} (${exp.tipoItse || ''})` : 'Pendiente de inspección ITSE';
    document.getElementById('resMontoTasa').innerText = exp.montoTasa ? `S/. ${exp.montoTasa.toFixed(2)}` : 'Por calcular';

    const alertaTexto = exp.alertaVencimiento ? ' ⚠️ (PRÓXIMO A VENCER)' : '';
    document.getElementById('resPlazoRestante').innerText = `${exp.diasHabilesRestantes} días hábiles restantes${alertaTexto}`;
    document.getElementById('resPlazoRestante').style.color = exp.alertaVencimiento ? 'var(--danger)' : 'var(--text-main)';

    // Badge de estado
    const badgeEl = document.getElementById('resEstadoBadge');
    badgeEl.className = 'badge ' + getBadgeClass(exp.estado);
    badgeEl.innerText = exp.estado.replace(/_/g, ' ');

    // Timeline
    actualizarTimeline(exp.estado);

    // Botones de descarga
    const btnDj = document.getElementById('btnDescargarDj');
    if (btnDj) btnDj.href = `${API_BASE}/${exp.id}/documentos/declaracion-jurada`;

    const btnVch = document.getElementById('btnDescargarVoucher');
    if (btnVch) {
      btnVch.style.display = exp.montoTasa ? 'inline-flex' : 'none';
      if (exp.montoTasa) btnVch.href = `${API_BASE}/${exp.id}/documentos/voucher-sat`;
    }

    // Licencia aprobada
    const boxLicencia = document.getElementById('boxLicenciaAprobada');
    const btnLic      = document.getElementById('btnDescargarLicencia');
    const btnVerif    = document.getElementById('btnVerificarPublico');

    if (exp.estado === 'APROBADO' && exp.licenciaQrCode) {
      if (boxLicencia) {
        boxLicencia.style.display = 'flex';
        document.getElementById('txtCodigoLicencia').innerText = `Licencia N°: ${exp.licenciaQrCode}`;
        document.getElementById('imgQrLicencia').src = `${API_BASE}/${exp.id}/qr?size=130`;
      }
      if (btnLic)   { btnLic.style.display  = 'inline-flex'; btnLic.href  = `${API_BASE}/${exp.id}/documentos/licencia`; }
      if (btnVerif) { btnVerif.style.display = 'inline-flex'; btnVerif.href = `verificar-licencia.html?codigo=${encodeURIComponent(exp.licenciaQrCode)}`; }
    } else {
      if (boxLicencia) boxLicencia.style.display = 'none';
      if (btnLic)   btnLic.style.display   = 'none';
      if (btnVerif) btnVerif.style.display  = 'none';
    }

    // Historial
    cargarHistorial(exp.id);

  } catch (err) {
    alert(err.message);
    document.getElementById('resultadoConsulta').style.display = 'none';
  }
}

// =============================================================================
// HELPERS
// =============================================================================
function actualizarTimeline(estado) {
  const steps = ['FORMATOS_GENERADOS', 'DOCUMENTOS_VALIDADOS', 'EN_EVALUACION_FINAL', 'APROBADO'];
  const currentIndex = steps.indexOf(estado);

  steps.forEach((st, idx) => {
    const el = document.getElementById(`step-${st}`);
    if (!el) return;
    el.classList.remove('active', 'completed');
    if (idx < currentIndex)      el.classList.add('completed');
    else if (idx === currentIndex) el.classList.add('active');
  });
}

function getBadgeClass(estado) {
  switch (estado) {
    case 'FORMATOS_GENERADOS':  return 'badge-formatos';
    case 'DOCUMENTOS_VALIDADOS': return 'badge-validados';
    case 'EN_EVALUACION_FINAL': return 'badge-evaluacion';
    case 'APROBADO':            return 'badge-aprobado';
    case 'RECHAZADO':           return 'badge-rechazado';
    default:                    return 'badge-formatos';
  }
}

async function cargarHistorial(expedienteId) {
  const tbody = document.getElementById('tablaHistorialBody');
  tbody.innerHTML = '<tr><td colspan="5">Cargando historial...</td></tr>';

  try {
    const res  = await fetch(`${API_BASE}/${expedienteId}/historial`);
    const list = await res.json();

    if (!list || list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5">Sin transiciones registradas.</td></tr>';
      return;
    }

    tbody.innerHTML = list.map(h => `
      <tr>
        <td>${new Date(h.fecha).toLocaleString()}</td>
        <td>${h.estadoAnterior ? `<span class="badge ${getBadgeClass(h.estadoAnterior)}">${h.estadoAnterior.replace(/_/g,' ')}</span>` : '<em>Inicio</em>'}</td>
        <td><span class="badge ${getBadgeClass(h.estadoNuevo)}">${h.estadoNuevo.replace(/_/g,' ')}</span></td>
        <td><strong>${h.usuario}</strong></td>
        <td>${h.motivo}</td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="5">Error al cargar historial.</td></tr>';
  }
}

// =============================================================================
// CARGA RÁPIDA DE DATOS DEMO (FACILITADOR DE PRUEBAS)
// =============================================================================
function llenarDatosDemo() {
  // Paso 1
  document.getElementById('nombreTitular').value = 'Ing. Wilder Palomino Gómez';
  document.getElementById('documentoIdentidad').value = '47829103';
  document.getElementById('razonSocial').value = 'INVERSIONES SAN CRISTÓBAL S.A.C.';
  document.getElementById('telefono').value = '966123456';
  document.getElementById('correoElectronico').value = 'admin@sancristobalayacucho.com';

  // Paso 2
  document.getElementById('nombreComercial').value = 'Café Cultural Wari Ayacucho';
  document.getElementById('giroNegocio').value = 'Venta de café y productos típicos artesanales';
  document.getElementById('direccionEstablecimiento').value = 'Jr. 28 de Julio N° 245 - Centro Histórico, Huamanga';
  document.getElementById('areaMetrosCuadrados').value = '95.5';

  alert('✅ Datos de prueba cargados con éxito en los Pasos 1 y 2. Ahora puede hacer clic en "Siguiente" para probar la interacción.');
}

