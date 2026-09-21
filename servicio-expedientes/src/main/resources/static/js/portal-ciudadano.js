const API_BASE = '/api/expedientes';

function mostrarPestana(tab) {
  const secRegistro = document.getElementById('secRegistro');
  const secSeguimiento = document.getElementById('secSeguimiento');
  const btnRegistro = document.getElementById('tabBtnRegistro');
  const btnSeguimiento = document.getElementById('tabBtnSeguimiento');

  if (tab === 'registro') {
    secRegistro.style.display = 'block';
    secSeguimiento.style.display = 'none';
    btnRegistro.className = 'btn btn-primary';
    btnSeguimiento.className = 'btn btn-secondary';
  } else {
    secRegistro.style.display = 'none';
    secSeguimiento.style.display = 'block';
    btnRegistro.className = 'btn btn-secondary';
    btnSeguimiento.className = 'btn btn-primary';
    consultarTramite();
  }
}

async function registrarSolicitud(e) {
  e.preventDefault();
  const btn = document.getElementById('btnEnviar');
  btn.disabled = true;
  btn.innerText = 'Enviando...';

  const doc = document.getElementById('documentoIdentidad').value.trim();
  const regexDoc = /^([0-9]{8}|(10|20)[0-9]{9})$/;
  if (!regexDoc.test(doc)) {
    alert('Error: El documento debe ser un DNI de 8 dígitos o un RUC de 11 dígitos iniciado con 10 o 20.');
    btn.disabled = false;
    btn.innerText = '🚀 Enviar Solicitud a Mesa de Partes';
    return;
  }

  const payload = {
    solicitanteId: crypto.randomUUID ? crypto.randomUUID() : 'f47ac10b-58cc-4372-a567-0e02b2c3d479',
    nombreTitular: document.getElementById('nombreTitular').value.trim(),
    documentoIdentidad: doc,
    razonSocial: document.getElementById('razonSocial').value.trim() || null,
    nombreComercial: document.getElementById('nombreComercial').value.trim(),
    giroNegocio: document.getElementById('giroNegocio').value.trim(),
    direccionEstablecimiento: document.getElementById('direccionEstablecimiento').value.trim(),
    areaMetrosCuadrados: parseFloat(document.getElementById('areaMetrosCuadrados').value),
    telefono: document.getElementById('telefono').value.trim(),
    correoElectronico: document.getElementById('correoElectronico').value.trim()
  };

  try {
    const res = await fetch(API_BASE, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || 'Error al procesar solicitud');
    }

    const exp = await res.json();
    alert(`✅ ¡Solicitud registrada exitosamente!\n\nNúmero de Trámite asignado: ${exp.numeroTramite}\nPlazo legal máximo: 15 días hábiles.\n\nPuede consultar su expediente en la pestaña de Seguimiento.`);

    document.getElementById('formRegistro').reset();
    document.getElementById('inputBuscarTramite').value = exp.numeroTramite;
    mostrarPestana('seguimiento');
  } catch (err) {
    alert('❌ Error al registrar solicitud: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.innerText = '🚀 Enviar Solicitud a Mesa de Partes';
  }
}

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
    badgeEl.innerText = exp.estado.replace('_', ' ');

    // Actualizar timeline
    actualizarTimeline(exp.estado);

    // Cargar Historial
    cargarHistorial(exp.id);

  } catch (err) {
    alert(err.message);
    document.getElementById('resultadoConsulta').style.display = 'none';
  }
}

function actualizarTimeline(estado) {
  const steps = ['FORMATOS_GENERADOS', 'DOCUMENTOS_VALIDADOS', 'EN_EVALUACION_FINAL', 'APROBADO'];
  const currentIndex = steps.indexOf(estado);

  steps.forEach((st, idx) => {
    const el = document.getElementById(`step-${st}`);
    if (!el) return;
    el.classList.remove('active', 'completed');
    if (idx < currentIndex) {
      el.classList.add('completed');
    } else if (idx === currentIndex) {
      el.classList.add('active');
    }
  });
}

function getBadgeClass(estado) {
  switch (estado) {
    case 'FORMATOS_GENERADOS': return 'badge-formatos';
    case 'DOCUMENTOS_VALIDADOS': return 'badge-validados';
    case 'EN_EVALUACION_FINAL': return 'badge-evaluacion';
    case 'APROBADO': return 'badge-aprobado';
    case 'RECHAZADO': return 'badge-rechazado';
    default: return 'badge-formatos';
  }
}

async function cargarHistorial(expedienteId) {
  const tbody = document.getElementById('tablaHistorialBody');
  tbody.innerHTML = '<tr><td colspan="5">Cargando historial...</td></tr>';

  try {
    const res = await fetch(`${API_BASE}/${expedienteId}/historial`);
    const list = await res.json();

    if (!list || list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5">Sin transiciones registradas.</td></tr>';
      return;
    }

    tbody.innerHTML = list.map(h => `
      <tr>
        <td>${new Date(h.fecha).toLocaleString()}</td>
        <td>${h.estadoAnterior ? `<span class="badge ${getBadgeClass(h.estadoAnterior)}">${h.estadoAnterior}</span>` : '<em>Inicio</em>'}</td>
        <td><span class="badge ${getBadgeClass(h.estadoNuevo)}">${h.estadoNuevo}</span></td>
        <td><strong>${h.usuario}</strong></td>
        <td>${h.motivo}</td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="5">Error al cargar historial.</td></tr>';
  }
}
