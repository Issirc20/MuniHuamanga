const API_BASE        = '/api/expedientes';
const API_FORMULARIOS = '/api/formularios';
const API_AUTH        = '/api/auth';
const API_TUPA        = '/api/tupa/tarifas';

document.addEventListener('DOMContentLoaded', () => {
  const demoBox = document.getElementById('demoCredentialsBox');
  if (demoBox && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
    demoBox.style.display = 'none';
  }
  verificarSesion();
  cargarExpedientes();
});

/* ══════════════════════════════════════════════════════════════════════════
   GESTIÓN DE SESIÓN Y SEGURIDAD JWT (Fase 04 Sprint 4-C)
   ══════════════════════════════════════════════════════════════════════════ */

function verificarSesion() {
  const token = localStorage.getItem('jwt_token');
  const usuario = localStorage.getItem('jwt_usuario');
  const rol = localStorage.getItem('jwt_rol');

  const label = document.getElementById('labelUsuario');
  const btnCerrar = document.getElementById('btnCerrarSesion');
  const btnIniciar = document.getElementById('btnIniciarSesion');
  const modalLogin = document.getElementById('modalLogin');

  if (token && usuario) {
    const rolCorto = rol ? rol.replace('ROLE_', '') : 'USUARIO';
    if (label) {
      label.innerHTML = `👤 <strong>${usuario}</strong> <span style="font-size: 0.75rem; background: rgba(255,255,255,0.25); padding: 0.15rem 0.4rem; border-radius: 4px; margin-left: 0.25rem;">${rolCorto}</span>`;
      label.style.display = 'inline-block';
    }
    if (btnCerrar) btnCerrar.style.display = 'inline-block';
    if (btnIniciar) btnIniciar.style.display = 'none';
    if (modalLogin) modalLogin.style.display = 'none';
  } else {
    if (label) label.style.display = 'none';
    if (btnCerrar) btnCerrar.style.display = 'none';
    if (btnIniciar) btnIniciar.style.display = 'inline-block';
  }
}

function abrirModalLogin() {
  const modal = document.getElementById('modalLogin');
  if (modal) modal.style.display = 'flex';
}

function setCredencialesRapidas(usuario, password) {
  document.getElementById('txtUsuario').value = usuario;
  document.getElementById('txtPassword').value = password;
  iniciarSesion();
}

async function iniciarSesion() {
  const txtU = document.getElementById('txtUsuario');
  const txtP = document.getElementById('txtPassword');
  const errorBox = document.getElementById('loginError');

  const username = txtU.value.trim();
  const password = txtP.value.trim();

  if (!username || !password) {
    errorBox.innerText = 'Por favor complete usuario y contraseña.';
    errorBox.style.display = 'block';
    return;
  }

  errorBox.style.display = 'none';

  try {
    const res = await fetch(`${API_AUTH}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Credenciales inválidas. Verifique usuario o contraseña.');
    }

    const data = await res.json();
    localStorage.setItem('jwt_token', data.token);
    localStorage.setItem('jwt_usuario', data.nombreCompleto || data.username);
    localStorage.setItem('jwt_rol', data.rol);

    verificarSesion();
    cargarExpedientes();
  } catch (err) {
    errorBox.innerText = err.message;
    errorBox.style.display = 'block';
  }
}

function cerrarSesion() {
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('jwt_usuario');
  localStorage.removeItem('jwt_rol');
  verificarSesion();
  cargarExpedientes();
}

/**
 * Cliente HTTP seguro que inyecta automáticamente el token JWT Bearer
 * y gestiona códigos 401 (no autenticado) y 403 (permiso denegado por rol).
 */
async function fetchConAuth(url, options = {}) {
  const token = localStorage.getItem('jwt_token');

  options.headers = options.headers || {};
  if (token) {
    if (options.headers instanceof Headers) {
      options.headers.set('Authorization', `Bearer ${token}`);
    } else {
      options.headers['Authorization'] = `Bearer ${token}`;
    }
  }

  const res = await fetch(url, options);

  if (res.status === 401) {
    abrirModalLogin();
    throw new Error('Debe iniciar sesión para realizar esta operación administrativa.');
  }

  if (res.status === 403) {
    const rolActual = localStorage.getItem('jwt_rol') || 'USUARIO';
    throw new Error(`⛔ Acceso Denegado: Su rol actual (${rolActual.replace('ROLE_', '')}) no cuenta con los permisos necesarios para realizar esta operación.`);
  }

  return res;
}

/* ══════════════════════════════════════════════════════════════════════════
   BANDEJA DE TRÁMITES Y KPIS
   ══════════════════════════════════════════════════════════════════════════ */

async function cargarExpedientes(soloAlerta = false) {
  const tbody = document.getElementById('tablaExpedientesBody');
  tbody.innerHTML = '<tr><td colspan="9">Cargando trámites...</td></tr>';

  const estadoFiltro = document.getElementById('filtroEstado').value;
  let url = API_BASE;
  const params = [];
  if (estadoFiltro) params.push(`estado=${estadoFiltro}`);
  if (soloAlerta) params.push(`conAlerta=true`);
  if (params.length > 0) url += '?' + params.join('&');

  try {
    const res = await fetchConAuth(url);
    const expedientes = await res.json();

    actualizarKpis(expedientes);

    if (!expedientes || expedientes.length === 0) {
      tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted);">No se encontraron expedientes.</td></tr>';
      return;
    }

    tbody.innerHTML = expedientes.map(exp => {
      const alertaBadge = exp.alertaVencimiento && exp.estado !== 'APROBADO' && exp.estado !== 'RECHAZADO'
        ? `<span class="badge badge-alert">⚠️ ${exp.diasHabilesRestantes}d hábiles</span>`
        : `<span>${exp.diasHabilesRestantes}d hábiles</span>`;

      return `
        <tr>
          <td><strong style="color: var(--primary);">${exp.numeroTramite}</strong></td>
          <td>
            <strong>${exp.nombreTitular}</strong><br>
            <span style="font-size: 0.8rem; color: var(--text-muted);">${exp.razonSocial || 'Persona Natural'} (${exp.documentoIdentidad})</span>
          </td>
          <td>
            ${exp.giroNegocio}<br>
            <span style="font-size: 0.8rem; color: var(--text-muted);">${exp.direccionEstablecimiento}</span>
          </td>
          <td><span class="badge ${getBadgeClass(exp.estado)}">${exp.estado.replace('_', ' ')}</span></td>
          <td>${exp.nivelRiesgo ? `<strong>${exp.nivelRiesgo}</strong> <span style="font-size: 0.75rem; color: var(--text-muted);">(${exp.tipoItse || ''})</span>` : '<span style="color: var(--text-muted);">Pendiente</span>'}</td>
          <td>${exp.montoTasa ? `<strong style="color: var(--success);">S/. ${exp.montoTasa.toFixed(2)}</strong>` : '-'}</td>
          <td>${alertaBadge}</td>
          <td>
            <div style="display: flex; gap: 0.25rem; flex-wrap: wrap;">
              ${renderFormatosPdf(exp)}
            </div>
          </td>
          <td>
            <div style="display: flex; gap: 0.25rem; flex-wrap: wrap;">
              ${renderAcciones(exp)}
              <button class="btn btn-secondary btn-sm" onclick="verHistorial('${exp.id}')">📜 Trazabilidad</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="9" style="color: var(--danger); text-align: center;">${err.message}</td></tr>`;
  }
}

function renderFormatosPdf(exp) {
  let html = `
    <a href="${API_BASE}/${exp.id}/documentos/declaracion-jurada" target="_blank" class="btn btn-secondary btn-sm" title="Descargar Anexo 1">
      📄 Anexo 1
    </a>
  `;

  if (exp.nivelRiesgo) {
    html += `
      <a href="${API_BASE}/${exp.id}/documentos/anexo3-matriz-riesgo-itse" target="_blank" class="btn btn-secondary btn-sm" title="Descargar Anexo 3 - Reporte de Riesgo ITSE">
        🛡️ Anexo 3
      </a>
      <a href="${API_BASE}/${exp.id}/documentos/anexo4-condiciones-seguridad" target="_blank" class="btn btn-secondary btn-sm" title="Descargar Anexo 4 - Declaración de Condiciones de Seguridad">
        📋 Anexo 4
      </a>
    `;
  }

  if (exp.voucherId || exp.voucherSatId) {
    html += `
      <a href="${API_BASE}/${exp.id}/documentos/voucher-sat" target="_blank" class="btn btn-secondary btn-sm" style="color: #0369a1;" title="Descargar Voucher SAT Huamanga">
        🧾 Voucher SAT
      </a>
    `;
  }

  if (exp.estado === 'APROBADO') {
    html += `
      <a href="${API_BASE}/${exp.id}/documentos/licencia" target="_blank" class="btn btn-primary btn-sm" title="Certificado Oficial de Licencia">
        🎖️ Licencia Oficial
      </a>
      <a href="${API_BASE}/${exp.id}/documentos/licencia-qr" target="_blank" class="btn btn-secondary btn-sm" title="Descargar Imagen QR">
        📱 QR
      </a>
    `;
  }

  return html;
}

function renderAcciones(exp) {
  if (exp.estado === 'FORMATOS_GENERADOS') {
    return `
      <button class="btn btn-primary btn-sm" onclick="abrirModalItse('${exp.id}')">
        🛡️ Dictaminar ITSE
      </button>
      <button class="btn btn-warning btn-sm" onclick="abrirModalObservacion('${exp.id}')" title="Formular observaciones según LPAG">
        ⚠️ Observar
      </button>
      <button class="btn btn-danger btn-sm" onclick="abrirModalRechazo('${exp.id}')">
        ❌ Rechazar
      </button>
    `;
  }

  if (exp.estado === 'DOCUMENTOS_VALIDADOS') {
    const vchId = exp.voucherId || exp.voucherSatId || '';
    return `
      <button class="btn btn-secondary btn-sm" onclick="generarVoucherSat('${exp.id}')" title="Generar código de voucher SAT">
        🧾 Generar Voucher
      </button>
      <button class="btn btn-success btn-sm" onclick="abrirModalPago('${exp.id}', '${vchId}')">
        💳 Validar Pago
      </button>
      <button class="btn btn-warning btn-sm" onclick="abrirModalObservacion('${exp.id}')" title="Formular observaciones según LPAG">
        ⚠️ Observar
      </button>
      <button class="btn btn-danger btn-sm" onclick="abrirModalRechazo('${exp.id}')">
        ❌ Rechazar
      </button>
    `;
  }

  if (exp.estado === 'EN_EVALUACION_FINAL') {
    return `
      <button class="btn btn-primary btn-sm" onclick="abrirEvaluacionAdmin('${exp.id}')" title="Formular informe administrativo técnico">
        📋 Informe Técnico (I07)
      </button>
      <button class="btn btn-warning btn-sm" onclick="abrirModalObservacion('${exp.id}')" title="Formular observaciones según LPAG">
        ⚠️ Observar
      </button>
      <button class="btn btn-danger btn-sm" onclick="abrirModalRechazo('${exp.id}')">
        ❌ Rechazar
      </button>
    `;
  }

  if (exp.estado === 'APTO_PARA_RESOLUCION') {
    return `
      <button class="btn btn-primary btn-sm" onclick="abrirBandejaResoluciones()" title="Gestionar resolución gerencial y firma digital">
        📜 Resoluciones (I08)
      </button>
      <button class="btn btn-warning btn-sm" onclick="abrirModalObservacion('${exp.id}')" title="Formular observaciones según LPAG">
        ⚠️ Observar
      </button>
    `;
  }

  if (exp.estado === 'OBSERVADO') {
    return `
      <button class="btn btn-primary btn-sm" onclick="abrirModalSubsanacion('${exp.id}')" title="Registrar subsanación presentada por el administrado">
        📝 Subsanar
      </button>
      <button class="btn btn-danger btn-sm" onclick="abrirModalRechazo('${exp.id}')" title="Desestimar por vencimiento de plazo de subsanación">
        ❌ Denegar
      </button>
    `;
  }

  if (exp.estado === 'APROBADO') {
    return `
      <a href="verificar-licencia.html?codigo=${exp.licenciaQrCode || ''}" target="_blank" class="btn btn-secondary btn-sm">
        🔍 Verificación Pública
      </a>
    `;
  }
  return '';
}

function actualizarKpis(list) {
  document.getElementById('kpiTotal').innerText = list.length;
  document.getElementById('kpiTramite').innerText = list.filter(e => e.estado !== 'APROBADO' && e.estado !== 'RECHAZADO').length;
  document.getElementById('kpiAprobados').innerText = list.filter(e => e.estado === 'APROBADO').length;
  document.getElementById('kpiAlertas').innerText = list.filter(e => e.alertaVencimiento && e.estado !== 'APROBADO' && e.estado !== 'RECHAZADO').length;
}

function getBadgeClass(estado) {
  switch (estado) {
    case 'FORMATOS_GENERADOS': return 'badge-formatos';
    case 'DOCUMENTOS_VALIDADOS': return 'badge-validados';
    case 'EN_EVALUACION_FINAL': return 'badge-evaluacion';
    case 'OBSERVADO': return 'badge-observado';
    case 'APROBADO': return 'badge-aprobado';
    case 'RECHAZADO': return 'badge-rechazado';
    default: return 'badge-formatos';
  }
}

/* Modales y Operaciones con Control de Roles */

function abrirModalItse(id) {
  document.getElementById('itseExpedienteId').value = id;
  document.getElementById('modalItse').style.display = 'flex';
}

async function guardarClasificacionItse() {
  const id = document.getElementById('itseExpedienteId').value;
  const nivel = document.getElementById('selectNivelRiesgo').value;
  const informe = document.getElementById('txtInformeItse').value;
  const obs = document.getElementById('txtObsItse').value;
  const usuario = localStorage.getItem('jwt_usuario') || 'EVALUADOR_ITSE';

  try {
    const res = await fetchConAuth(`${API_BASE}/${id}/clasificar-riesgo`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nivelRiesgo: nivel, informeItseNumero: informe, observaciones: obs, usuario: usuario })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al registrar dictamen ITSE');
    }
    cerrarModal('modalItse');
    cargarExpedientes();
  } catch (err) {
    alert(err.message);
  }
}

async function generarVoucherSat(id) {
  try {
    const res = await fetchConAuth(`${API_BASE}/${id}/voucher`, { method: 'POST' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al generar voucher');
    }
    const vch = await res.json();
    alert(`🧾 Voucher SAT Generado exitosamente:\n\nCódigo: ${vch.voucherId}\nMonto: S/. ${vch.monto.toFixed(2)}\nCódigo de Barras: ${vch.codigoBarrasSat}`);
    cargarExpedientes();
  } catch (err) {
    alert(err.message);
  }
}

function abrirModalPago(id, voucherId) {
  document.getElementById('pagoExpedienteId').value = id;
  document.getElementById('txtVoucherId').value = voucherId || 'VCH-2026-000123';
  document.getElementById('modalPago').style.display = 'flex';
}

async function confirmarPagoSat() {
  const id = document.getElementById('pagoExpedienteId').value;
  const voucher = document.getElementById('txtVoucherId').value.trim();
  const op = document.getElementById('txtOperacionSat').value.trim();
  const usuario = localStorage.getItem('jwt_usuario') || 'CAJA_SAT';

  try {
    const res = await fetchConAuth(`${API_BASE}/${id}/pago`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ voucherId: voucher, numeroOperacionSat: op, montoPagado: 218.00, usuario: usuario })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al registrar pago');
    }
    cerrarModal('modalPago');
    cargarExpedientes();
  } catch (err) {
    alert(err.message);
  }
}

async function aprobarExpediente(id) {
  if (!confirm('¿Confirma la APROBACIÓN del expediente y la emisión de la Licencia Digital con Código QR?')) return;
  const usuario = localStorage.getItem('jwt_usuario') || 'GERENCIA_LICENCIAS';

  try {
    const res = await fetchConAuth(`${API_BASE}/${id}/aprobar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ aprobado: true, motivo: 'Cumple con los requisitos normativos Ley 28976', funcionarioResponsable: usuario })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al aprobar expediente');
    }
    alert('✅ Expediente aprobado exitosamente. Licencia emitida con código QR único.');
    cargarExpedientes();
  } catch (err) {
    alert('❌ ' + err.message);
  }
}

function abrirModalRechazo(id) {
  document.getElementById('rechazoExpedienteId').value = id;
  document.getElementById('modalRechazo').style.display = 'flex';
}

async function confirmarRechazo() {
  const id = document.getElementById('rechazoExpedienteId').value;
  const motivo = document.getElementById('txtMotivoRechazo').value.trim();
  if (!motivo) return alert('El motivo es obligatorio');
  const usuario = localStorage.getItem('jwt_usuario') || 'GERENCIA_LICENCIAS';

  try {
    const res = await fetchConAuth(`${API_BASE}/${id}/rechazar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ aprobado: false, motivo: motivo, funcionarioResponsable: usuario })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al rechazar expediente');
    }
    cerrarModal('modalRechazo');
    cargarExpedientes();
  } catch (err) {
    alert(err.message);
  }
}

function abrirModalObservacion(id) {
  document.getElementById('observacionExpedienteId').value = id;
  document.getElementById('txtMotivoObservacion').value = '';
  document.getElementById('modalObservacion').style.display = 'flex';
}

async function confirmarObservacion() {
  const id = document.getElementById('observacionExpedienteId').value;
  const motivo = document.getElementById('txtMotivoObservacion').value.trim();
  if (!motivo) return alert('El detalle de las observaciones es obligatorio');
  const usuario = localStorage.getItem('jwt_usuario') || 'GERENCIA_LICENCIAS';

  try {
    const res = await fetchConAuth(`${API_BASE}/${id}/observar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ aprobado: false, motivo: motivo, funcionarioResponsable: usuario })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al observar expediente');
    }
    cerrarModal('modalObservacion');
    cargarExpedientes();
    alert('✅ Expediente marcado como OBSERVADO. Se notificó al administrado para su subsanación.');
  } catch (err) {
    alert(err.message);
  }
}

function abrirModalSubsanacion(id) {
  document.getElementById('subsanacionExpedienteId').value = id;
  document.getElementById('txtDetalleSubsanacion').value = '';
  document.getElementById('modalSubsanacion').style.display = 'flex';
}

async function confirmarSubsanacion() {
  const id = document.getElementById('subsanacionExpedienteId').value;
  const detalle = document.getElementById('txtDetalleSubsanacion').value.trim();
  if (!detalle) return alert('El detalle de la subsanación es obligatorio');
  const usuario = localStorage.getItem('jwt_usuario') || 'MESA_DE_PARTES';

  try {
    const res = await fetchConAuth(`${API_BASE}/${id}/subsanar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ detalleSubsanacion: detalle, usuario: usuario })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al registrar subsanación');
    }
    cerrarModal('modalSubsanacion');
    cargarExpedientes();
    alert('✅ Subsanación registrada con éxito. El expediente retorna al flujo de evaluación.');
  } catch (err) {
    alert(err.message);
  }
}

async function verHistorial(id) {
  const tbody = document.getElementById('tablaModalHistorial');
  tbody.innerHTML = '<tr><td colspan="4">Cargando...</td></tr>';
  document.getElementById('modalHistorial').style.display = 'flex';

  try {
    const res = await fetchConAuth(`${API_BASE}/${id}/historial`);
    const list = await res.json();
    tbody.innerHTML = list.map(h => `
      <tr>
        <td>${new Date(h.fecha).toLocaleString()}</td>
        <td><span class="badge ${getBadgeClass(h.estadoNuevo)}">${h.estadoNuevo}</span></td>
        <td><strong>${h.usuario}</strong></td>
        <td>${h.motivo}</td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="4" style="color: var(--danger); text-align: center;">${err.message}</td></tr>`;
  }
}

function cerrarModal(modalId) {
  document.getElementById(modalId).style.display = 'none';
}

/* ══════════════════════════════════════════════════════════════════════════
   GESTIÓN DEL TARIFARIO TUPA MUNICIPAL (Fase 04 Sprint 4-D)
   ══════════════════════════════════════════════════════════════════════════ */

let tarifasTupaCargadas = [];

async function abrirModalTupa() {
  document.getElementById('modalTupa').style.display = 'flex';
  const tbody = document.getElementById('tablaTupaBody');
  tbody.innerHTML = '<tr><td colspan="8" style="text-align: center;">Cargando tarifario oficial...</td></tr>';

  const rol = localStorage.getItem('jwt_rol');
  const esAdmin = rol === 'ROLE_ADMIN';
  const hint = document.getElementById('hintRolTupa');
  if (hint) {
    hint.innerHTML = esAdmin
      ? '🟢 Modo <strong>ADMINISTRADOR</strong>: Puede modificar los montos y desgloses de las tasas.'
      : 'ℹ️ Modo <strong>CONSULTA</strong>: La edición de tasas está reservada para el rol ADMINISTRADOR.';
  }

  try {
    const res = await fetchConAuth(API_TUPA);
    tarifasTupaCargadas = await res.json();

    if (!tarifasTupaCargadas || tarifasTupaCargadas.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center;">No hay tarifas registradas.</td></tr>';
      return;
    }

    tbody.innerHTML = tarifasTupaCargadas.map(t => {
      const botonEditar = esAdmin
        ? `<button class="btn btn-secondary btn-sm" onclick="abrirModalEditarTarifa('${t.id}')">✏️ Editar</button>`
        : `<span class="badge" style="background: #e2e8f0; color: #64748b;">🔒 Lectura</span>`;

      return `
        <tr>
          <td><strong style="color: var(--primary);">${t.codigoTupa}</strong></td>
          <td><strong>${t.nivelRiesgo}</strong></td>
          <td><strong style="color: var(--success); font-size: 1.05rem;">S/. ${t.montoTotal.toFixed(2)}</strong></td>
          <td>S/. ${t.derechoTramite.toFixed(2)}</td>
          <td>S/. ${t.costoItse.toFixed(2)}</td>
          <td><span style="font-size: 0.8rem; color: var(--text-muted);">${t.baseLegal || '—'}</span></td>
          <td>
            <span class="badge ${t.activo ? 'badge-aprobado' : 'badge-rechazado'}">
              ${t.activo ? 'Vigente' : 'Inactivo'}
            </span>
          </td>
          <td>${botonEditar}</td>
        </tr>
      `;
    }).join('');

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" style="color: var(--danger); text-align: center;">${err.message}</td></tr>`;
  }
}

function abrirModalEditarTarifa(tarifaId) {
  const tarifa = tarifasTupaCargadas.find(t => t.id === tarifaId);
  if (!tarifa) return;

  document.getElementById('editTarifaId').value = tarifa.id;
  document.getElementById('editTarifaRiesgo').value = `${tarifa.codigoTupa} — RIESGO ${tarifa.nivelRiesgo}`;
  document.getElementById('editTarifaMonto').value = tarifa.montoTotal;
  document.getElementById('editTarifaTramite').value = tarifa.derechoTramite;
  document.getElementById('editTarifaItse').value = tarifa.costoItse;
  document.getElementById('editTarifaBaseLegal').value = tarifa.baseLegal || '';
  document.getElementById('editTarifaConcepto').value = tarifa.concepto || '';

  document.getElementById('modalEditarTarifa').style.display = 'flex';
}

function recalcularDesgloseEdit() {
  const total = parseFloat(document.getElementById('editTarifaMonto').value) || 0;
  const tramite = parseFloat(document.getElementById('editTarifaTramite').value) || 0;
  const itse = Math.max(0, total - tramite);
  document.getElementById('editTarifaItse').value = itse.toFixed(2);
}

async function guardarEdicionTarifa() {
  const id = document.getElementById('editTarifaId').value;
  const total = parseFloat(document.getElementById('editTarifaMonto').value);
  const tramite = parseFloat(document.getElementById('editTarifaTramite').value) || 0;
  const itse = parseFloat(document.getElementById('editTarifaItse').value) || 0;
  const baseLegal = document.getElementById('editTarifaBaseLegal').value.trim();
  const concepto = document.getElementById('editTarifaConcepto').value.trim();

  if (isNaN(total) || total <= 0) {
    alert('Ingrese un monto total válido mayor a 0');
    return;
  }

  const payload = {
    montoTotal: total,
    derechoTramite: tramite,
    costoItse: itse,
    baseLegal: baseLegal,
    concepto: concepto,
    activo: true
  };

  try {
    const res = await fetchConAuth(`${API_TUPA}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al actualizar tarifa TUPA');
    }

    alert('✅ Tarifa TUPA actualizada exitosamente.');
    cerrarModal('modalEditarTarifa');
    abrirModalTupa();
  } catch (err) {
    alert('❌ ' + err.message);
  }
}

/* ══════════════════════════════════════════════════════════════════════════
   MÓDULO DE PAGOS SAT — VALIDACIÓN MANUAL (M05 - I06)
   ══════════════════════════════════════════════════════════════════════════ */

function abrirBandejaPagos() {
  const modal = document.getElementById('modalBandejaPagos');
  if (modal) {
    modal.style.display = 'flex';
    cargarBandejaPagos();
  }
}

async function cargarBandejaPagos() {
  const tbody = document.getElementById('tablaPagosBody');
  const filtroEstado = document.getElementById('filtroEstadoPago').value;
  if (!tbody) return;

  tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">Cargando comprobantes...</td></tr>';

  try {
    const url = filtroEstado ? `/api/pagos/bandeja?estado=${encodeURIComponent(filtroEstado)}` : '/api/pagos/bandeja';
    const res = await fetchConAuth(url);
    if (!res.ok) {
      if (res.status === 403) throw new Error('Acceso restringido: requiere rol CAJERO o ADMINISTRADOR.');
      throw new Error('Error al cargar comprobantes de pago.');
    }
    const lista = await res.json();
    if (!lista || lista.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No se encontraron comprobantes con el filtro seleccionado.</td></tr>';
      return;
    }

    tbody.innerHTML = lista.map(p => {
      let badgeColor = '#64748b';
      if (p.estado === 'PENDIENTE_VALIDACION') badgeColor = '#d97706';
      if (p.estado === 'EN_REVISION') badgeColor = '#2563eb';
      if (p.estado === 'VALIDADO_MANUALMENTE') badgeColor = '#16a34a';
      if (p.estado === 'OBSERVADO') badgeColor = '#ea580c';
      if (p.estado === 'RECHAZADO') badgeColor = '#dc2626';

      const acciones = p.estado === 'VALIDADO_MANUALMENTE'
        ? '<span style="color: #16a34a; font-weight: bold; font-size: 0.8rem;">✓ Validado</span>'
        : `
          <button class="btn btn-success btn-sm" onclick="abrirValidarPagoModal('${p.id}', '${p.montoDeclarado}')" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;">Validar</button>
          <button class="btn btn-warning btn-sm" onclick="observarPagoPrompt('${p.id}')" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;">Observar</button>
          <button class="btn btn-danger btn-sm" onclick="rechazarPagoPrompt('${p.id}')" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;">Rechazar</button>
        `;

      return `
        <tr>
          <td><strong>${p.codigoComprobante}</strong></td>
          <td>${p.fechaDeclarada || '—'}</td>
          <td>S/. ${(p.montoDeclarado != null ? Number(p.montoDeclarado).toFixed(2) : '0.00')}</td>
          <td>S/. ${(p.tasaTupaEsperada != null ? Number(p.tasaTupaEsperada).toFixed(2) : '—')}</td>
          <td><span style="background: ${badgeColor}; color: white; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: bold;">${p.estado}</span></td>
          <td>${p.validadorNombre || 'Pendiente'}</td>
          <td style="white-space: nowrap;">${acciones}</td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: #dc2626;">${err.message}</td></tr>`;
  }
}

function abrirValidarPagoModal(pagoId, montoDeclarado) {
  document.getElementById('actionPagoId').value = pagoId;
  document.getElementById('txtValMonto').value = montoDeclarado || '218.00';
  document.getElementById('txtValFechaReal').value = new Date().toISOString().split('T')[0];
  document.getElementById('txtValNumOperacion').value = 'OP-SAT-' + Math.floor(100000 + Math.random() * 900000);
  document.getElementById('modalValidarPagoAction').style.display = 'flex';
}

async function parsearErrorHttp(res, operacion) {
  const err = await res.json().catch(() => ({}));
  let detalle = err.message || '';
  if (err.detalles) {
    detalle = Object.entries(err.detalles).map(([k, v]) => `${k}: ${v}`).join(', ');
  }
  if (res.status === 400) {
    return `[HTTP 400 - Solicitud Inválida] ${detalle || 'Parámetros o formato de comprobante incorrecto.'}`;
  } else if (res.status === 401) {
    return `[HTTP 401 - No Autenticado] Sesión vencida o credenciales ausentes. Inicie sesión nuevamente.`;
  } else if (res.status === 403) {
    return `[HTTP 403 - Acceso Denegado] No cuenta con permisos para ${operacion} (Control BOLA/IDOR o rol no competente).`;
  } else if (res.status === 409) {
    return `[HTTP 409 - Conflicto] ${detalle || 'Conflicto de estado administrativo o concurrencia detectado.'}`;
  } else if (res.status === 422) {
    return `[HTTP 422 - No Procesable] Restricción de negocio no cumplida: ${detalle}`;
  }
  return `[HTTP ${res.status}] ${detalle || 'Error al procesar la solicitud municipal.'}`;
}

async function confirmarValidarPagoManual() {
  const pagoId = document.getElementById('actionPagoId').value;
  const numOp = document.getElementById('txtValNumOperacion').value.trim();
  const monto = document.getElementById('txtValMonto').value;
  const fecha = document.getElementById('txtValFechaReal').value;
  const just = document.getElementById('txtValJustificacion').value.trim();

  if (!numOp || !monto || !fecha || !just) {
    alert('Todos los campos son obligatorios.');
    return;
  }

  if (just.length < 10) {
    alert('La justificación de cotejo manual debe tener al menos 10 caracteres.');
    return;
  }

  try {
    const res = await fetchConAuth(`/api/pagos/${pagoId}/validar-manual`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        numeroOperacionSat: numOp,
        montoValidado: parseFloat(monto),
        fechaPagoReal: fecha,
        justificacion: just
      })
    });

    if (!res.ok) {
      const msg = await parsearErrorHttp(res, 'validar el comprobante de pago');
      throw new Error(msg);
    }

    alert('✅ Pago validado manualmente exitosamente.');
    cerrarModal('modalValidarPagoAction');
    cargarBandejaPagos();
    cargarExpedientes();
  } catch (err) {
    alert('❌ ' + err.message);
  }
}

async function observarPagoPrompt(pagoId) {
  const motivo = prompt('Ingrese el motivo de la observación del comprobante de pago (mínimo 5 caracteres):');
  if (!motivo || motivo.trim().length < 5) {
    alert('El motivo debe tener al menos 5 caracteres.');
    return;
  }

  try {
    const res = await fetchConAuth(`/api/pagos/${pagoId}/observar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ motivo: motivo.trim() })
    });

    if (!res.ok) {
      const msg = await parsearErrorHttp(res, 'observar el comprobante de pago');
      throw new Error(msg);
    }

    alert('⚠️ Pago observado exitosamente.');
    cargarBandejaPagos();
  } catch (err) {
    alert('❌ ' + err.message);
  }
}

async function rechazarPagoPrompt(pagoId) {
  const motivo = prompt('Ingrese el motivo del RECHAZO del comprobante (mínimo 5 caracteres):');
  if (!motivo || motivo.trim().length < 5) {
    alert('El motivo debe tener al menos 5 caracteres.');
    return;
  }

  try {
    const res = await fetchConAuth(`/api/pagos/${pagoId}/rechazar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ motivo: motivo.trim() })
    });

    if (!res.ok) {
      const msg = await parsearErrorHttp(res, 'rechazar el comprobante de pago');
      throw new Error(msg);
    }

    alert('🚫 Pago rechazado.');
    cargarBandejaPagos();
  } catch (err) {
    alert('❌ ' + err.message);
  }
}

/* ══════════════════════════════════════════════════════════════════════════
   MÓDULO DE EVALUACIÓN DE ZONIFICACIÓN (M07 - I06)
   ══════════════════════════════════════════════════════════════════════════ */

function abrirBandejaZonificacion() {
  const primerExp = prompt('Ingrese el ID (UUID) del expediente a evaluar zonificación:');
  if (primerExp && primerExp.trim()) {
    abrirModalZonificacion(primerExp.trim());
  }
}

function abrirModalZonificacion(expedienteId) {
  document.getElementById('zonifExpedienteId').value = expedienteId;
  const modal = document.getElementById('modalZonificacionI06');
  if (modal) modal.style.display = 'flex';
}

async function guardarEvaluacionZonificacion() {
  const expId = document.getElementById('zonifExpedienteId').value;
  const resultado = document.getElementById('selectResultadoZonif').value;
  const refZonif = document.getElementById('txtRefZonificacion').value.trim();
  const fuente = document.getElementById('txtFuenteSustento').value.trim();
  const obs = document.getElementById('txtObsZonif').value.trim();

  if (!expId || !refZonif || !fuente || !obs) {
    alert('Todos los campos son obligatorios.');
    return;
  }

  if (fuente.length < 8) {
    alert('La fuente del informe o sustento técnico debe tener al menos 8 caracteres.');
    return;
  }

  if (obs.length < 15) {
    alert('La motivación de la evaluación debe tener al menos 15 caracteres.');
    return;
  }

  try {
    const res = await fetchConAuth(`/api/expedientes/${expId}/zonificacion/evaluar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        resultado: resultado,
        referenciaZonificacion: refZonif,
        fuenteSustentoTecnico: fuente,
        observaciones: obs
      })
    });

    if (!res.ok) {
      const msg = await parsearErrorHttp(res, 'registrar la evaluación de zonificación');
      throw new Error(msg);
    }

    alert('✅ Evaluación técnica de zonificación registrada exitosamente.');
    cerrarModal('modalZonificacionI06');
    cargarExpedientes();
  } catch (err) {
    alert('❌ ' + err.message);
  }
}

/* ══════════════════════════════════════════════════════════════════════════
   M08: EVALUACIÓN ADMINISTRATIVA, CHECKLIST E INFORMES TÉCNICOS (I07)
   ══════════════════════════════════════════════════════════════════════════ */

const API_EVAL = '/api/evaluaciones-administrativas';
let currentChecklistExpedienteId = null;
let currentGestionInformeId = null;

function abrirBandejaEvaluacion() {
  const modal = document.getElementById('modalBandejaEvaluacionI07');
  if (modal) modal.style.display = 'flex';
  cargarBandejaEvaluacion();
}

async function cargarBandejaEvaluacion() {
  const tbody = document.getElementById('tablaBandejaEvaluacionBody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="11" style="text-align: center;">Cargando bandeja de evaluación...</td></tr>';

  const busqueda = document.getElementById('filtroBandejaBusqueda')?.value?.trim();
  const estadoExp = document.getElementById('filtroBandejaEstadoExp')?.value;
  const pago = document.getElementById('filtroBandejaPago')?.value;
  const zonif = document.getElementById('filtroBandejaZonif')?.value;

  const params = new URLSearchParams();
  if (busqueda) params.append('busqueda', busqueda);
  if (estadoExp) params.append('estadoExpediente', estadoExp);
  if (pago) params.append('estadoPago', pago);
  if (zonif) params.append('resultadoZonificacion', zonif);
  params.append('page', '0');
  params.append('size', '20');

  try {
    const res = await fetchConAuth(`${API_EVAL}/bandeja?${params.toString()}`);
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al cargar la bandeja de evaluación');
    }

    const data = await res.json();
    const items = data.content || [];

    if (items.length === 0) {
      tbody.innerHTML = '<tr><td colspan="11" style="text-align: center; color: var(--text-muted);">No se encontraron expedientes para evaluación.</td></tr>';
      return;
    }

    tbody.innerHTML = items.map(item => {
      // Estado de Pago
      let pagoBadge = '<span class="status-badge" style="background:#fef3c7; color:#92400e;">PENDIENTE</span>';
      if (item.estadoPagoSat === 'VALIDADO_MANUALMENTE') {
        pagoBadge = '<span class="status-badge" style="background:#d1fae5; color:#065f46;">VALIDADO</span>';
      } else if (item.estadoPagoSat === 'RECHAZADO') {
        pagoBadge = '<span class="status-badge" style="background:#fee2e2; color:#991b1b;">RECHAZADO</span>';
      }

      // Zonificación
      let zonBadge = '<span class="status-badge" style="background:#fef3c7; color:#92400e;">PENDIENTE</span>';
      if (item.resultadoZonificacion === 'COMPATIBLE') {
        zonBadge = '<span class="status-badge" style="background:#d1fae5; color:#065f46;">COMPATIBLE</span>';
      } else if (item.resultadoZonificacion === 'NO_COMPATIBLE') {
        zonBadge = '<span class="status-badge" style="background:#fee2e2; color:#991b1b;">NO COMPATIBLE</span>';
      }

      // Alerta Plazo
      let alertaBadge = '<span class="status-badge" style="background:#d1fae5; color:#065f46;">EN PLAZO</span>';
      if (item.alertaPlazo === 'VENCIDO') {
        alertaBadge = '<span class="status-badge" style="background:#fee2e2; color:#991b1b; font-weight: bold;">VENCIDO</span>';
      } else if (item.alertaPlazo === 'PROXIMO_A_VENCER') {
        alertaBadge = '<span class="status-badge" style="background:#ffedd5; color:#c2410c;">PRÓXIMO</span>';
      }

      // Estado Informe
      let informeBadge = '<span class="hint">Sin informe</span>';
      if (item.informeEstado) {
        let colorBg = '#e0e7ff', colorTxt = '#3730a3';
        if (item.informeEstado === 'APROBADO') { colorBg = '#d1fae5'; colorTxt = '#065f46'; }
        else if (item.informeEstado === 'OBSERVADO') { colorBg = '#fee2e2'; colorTxt = '#991b1b'; }
        else if (item.informeEstado === 'VISADO') { colorBg = '#ecfdf5'; colorTxt = '#047857'; }
        informeBadge = `<span class="status-badge" style="background:${colorBg}; color:${colorTxt}; font-weight:600;">${item.informeEstado}</span>`;
      }

      const fPres = item.fechaPresentacion ? new Date(item.fechaPresentacion).toLocaleDateString() : '-';

      return `
        <tr>
          <td><strong>${item.numeroExpediente}</strong></td>
          <td>
            <div style="font-weight:600;">${item.titularNombre || '-'}</div>
            <div class="hint">${item.establecimientoNombre || ''}</div>
          </td>
          <td>${fPres}</td>
          <td><span class="status-badge" style="background:#f1f5f9; color:#334155;">${item.estadoExpediente}</span></td>
          <td>${pagoBadge}</td>
          <td>${zonBadge}</td>
          <td>
            <span class="status-badge" style="background:#ede9fe; color:#5b21b6;">${item.nivelRiesgoDeclarado || '-'}</span>
            <div class="hint">${item.sustentoItsePresente ? '✓ Sustento' : '✗ Sin sustento'}</div>
          </td>
          <td>${item.observacionesPendientes > 0 ? `<span style="color:var(--danger); font-weight:bold;">${item.observacionesPendientes}</span>` : '0'}</td>
          <td>${alertaBadge}</td>
          <td>${informeBadge}</td>
          <td style="white-space:nowrap;">
            <button class="btn btn-secondary btn-sm" title="Verificar Checklist Administrativo" onclick="verChecklistIntegral('${item.expedienteId}')">
              📋 Checklist
            </button>
            ${!item.informeId ? `
              <button class="btn btn-primary btn-sm" title="Elaborar Informe Técnico" onclick="abrirModalElaborarInforme('${item.expedienteId}')">
                📝 Elaborar
              </button>
            ` : `
              <button class="btn btn-secondary btn-sm" title="Ver / Gestionar Informe" onclick="verGestionInforme('${item.expedienteId}', '${item.informeId}')">
                📑 Informe
              </button>
            `}
          </td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="11" style="text-align: center; color: var(--danger);">Error: ${err.message}</td></tr>`;
  }
}

async function verChecklistIntegral(expedienteId) {
  currentChecklistExpedienteId = expedienteId;
  const tbody = document.getElementById('tablaChecklistBody');
  const resumenBox = document.getElementById('checklistHeaderResumen');
  tbody.innerHTML = '<tr><td colspan="4" style="text-align: center;">Evaluando requisitos normativos y documentales...</td></tr>';

  const modal = document.getElementById('modalChecklistIntegralI07');
  if (modal) modal.style.display = 'flex';

  try {
    const res = await fetchConAuth(`${API_EVAL}/expedientes/${expedienteId}/checklist`);
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al consultar checklist');
    }

    const data = await res.json();

    // Resumen header
    if (data.aptoParaInformeFavorable) {
      resumenBox.style.background = '#d1fae5';
      resumenBox.style.color = '#065f46';
      resumenBox.style.border = '1px solid #a7f3d0';
      resumenBox.innerHTML = `✅ <strong>EXPEDIENTE APTO PARA INFORME FAVORABLE:</strong> Todos los recaudos, pagos SAT, zonificación y sustento ITSE se encuentran conformes.`;
    } else {
      resumenBox.style.background = '#fee2e2';
      resumenBox.style.color = '#991b1b';
      resumenBox.style.border = '1px solid #fecaca';
      resumenBox.innerHTML = `⚠️ <strong>EXPEDIENTE NO APTO PARA INFORME FAVORABLE:</strong> ${data.motivoBloqueo || 'Presenta observaciones o requisitos pendientes'}`;
    }

    tbody.innerHTML = (data.items || []).map(it => {
      let badge = '<span class="status-badge" style="background:#fee2e2; color:#991b1b; font-weight:bold;">NO CUMPLE</span>';
      if (it.estado === 'CUMPLE') {
        badge = '<span class="status-badge" style="background:#d1fae5; color:#065f46; font-weight:bold;">CUMPLE</span>';
      } else if (it.estado === 'NO_APLICA') {
        badge = '<span class="status-badge" style="background:#f1f5f9; color:#64748b;">NO APLICA</span>';
      }

      return `
        <tr>
          <td><strong>${it.criterio}</strong></td>
          <td>${badge}</td>
          <td>${it.descripcion}</td>
          <td><span class="hint" style="color:#475569;">${it.fuenteEvidencia || '-'}</span></td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:var(--danger);">Error: ${err.message}</td></tr>`;
  }
}

function procederDesdeChecklist() {
  if (currentChecklistExpedienteId) {
    cerrarModal('modalChecklistIntegralI07');
    abrirModalElaborarInforme(currentChecklistExpedienteId);
  }
}

function abrirModalElaborarInforme(expedienteId) {
  document.getElementById('informeExpedienteId').value = expedienteId;
  document.getElementById('selectConclusionInforme').value = 'FAVORABLE';
  document.getElementById('txtFundamentosAdmin').value = '';
  document.getElementById('txtReferenciasDoc').value = '';
  document.getElementById('txtObservacionesInforme').value = '';

  const modal = document.getElementById('modalElaborarInformeI07');
  if (modal) modal.style.display = 'flex';
}

async function guardarInformeAdministrativo() {
  const expId = document.getElementById('informeExpedienteId').value;
  const conclusion = document.getElementById('selectConclusionInforme').value;
  const fundamentos = document.getElementById('txtFundamentosAdmin').value.trim();
  const referencias = document.getElementById('txtReferenciasDoc').value.trim();
  const obs = document.getElementById('txtObservacionesInforme').value.trim();

  if (!expId || !conclusion || !fundamentos || !referencias) {
    alert('Por favor complete todos los campos obligatorios (*).');
    return;
  }

  try {
    const res = await fetchConAuth(`${API_EVAL}/informes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        expedienteId: expId,
        conclusion: conclusion,
        fundamentosAdministrativos: fundamentos,
        referenciasDocumentales: referencias,
        observaciones: obs
      })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al guardar el informe administrativo');
    }

    const data = await res.json();
    alert(`✅ Informe Administrativo N° ${data.numeroInforme} guardado en estado ${data.estado}.`);
    cerrarModal('modalElaborarInformeI07');
    cargarBandejaEvaluacion();
  } catch (err) {
    alert(`❌ No se pudo guardar el informe:\n${err.message}`);
  }
}

async function verGestionInforme(expedienteId, informeId) {
  currentGestionInformeId = informeId;
  document.getElementById('gestionInformeId').value = informeId;
  document.getElementById('gestionExpedienteId').value = expedienteId;

  const box = document.getElementById('detallesInformeBox');
  const tbodyHistorial = document.getElementById('tablaHistorialInformeBody');
  box.innerHTML = 'Cargando datos del informe...';
  tbodyHistorial.innerHTML = '<tr><td colspan="6" style="text-align:center;">Cargando historial inmutable...</td></tr>';

  const modal = document.getElementById('modalGestionInformeI07');
  if (modal) modal.style.display = 'flex';

  try {
    const res = await fetchConAuth(`${API_EVAL}/informes/${informeId}`);
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al cargar informe');
    }

    const inf = await res.json();

    // Actualizar botones de acción según el estado
    const btnRev = document.getElementById('btnAccionEnviarRevision');
    const btnDev = document.getElementById('btnAccionDevolver');
    const btnVis = document.getElementById('btnAccionVisar');
    const btnApr = document.getElementById('btnAccionAprobar');

    btnRev.style.display = (inf.estado === 'BORRADOR' || inf.estado === 'OBSERVADO') ? 'inline-block' : 'none';
    btnDev.style.display = (inf.estado === 'EN_REVISION') ? 'inline-block' : 'none';
    btnVis.style.display = (inf.estado === 'EN_REVISION') ? 'inline-block' : 'none';
    btnApr.style.display = (inf.estado === 'VISADO') ? 'inline-block' : 'none';

    box.innerHTML = `
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.5rem; margin-bottom: 0.5rem;">
        <div><strong>Informe N°:</strong> ${inf.numeroInforme} (v${inf.version})</div>
        <div><strong>Estado:</strong> <span class="status-badge" style="background:#e0e7ff; color:#3730a3; font-weight:bold;">${inf.estado}</span></div>
        <div><strong>Conclusión:</strong> <span class="status-badge" style="background:#d1fae5; color:#065f46; font-weight:bold;">${inf.conclusion}</span></div>
        <div><strong>Evaluador:</strong> ${inf.evaluadorNombre}</div>
      </div>
      <div><strong>Fundamentos:</strong> ${inf.fundamentosAdministrativos}</div>
      <div style="margin-top:0.25rem;"><strong>Referencias:</strong> ${inf.referenciasDocumentales}</div>
      ${inf.observacionesRevision ? `<div style="margin-top:0.25rem; color:var(--danger);"><strong>Observaciones de Revisión:</strong> ${inf.observacionesRevision}</div>` : ''}
    `;

    // Cargar historial
    const resH = await fetchConAuth(`${API_EVAL}/informes/${informeId}/historial`);
    if (resH.ok) {
      const historial = await resH.json();
      tbodyHistorial.innerHTML = historial.map(h => `
        <tr>
          <td>${h.fechaAccion ? new Date(h.fechaAccion).toLocaleString() : '-'}</td>
          <td><strong>${h.accion}</strong></td>
          <td><span class="status-badge">${h.estadoResultante}</span></td>
          <td>${h.usuarioNombre || '-'}</td>
          <td>${h.rol || '-'}</td>
          <td>${h.observaciones || '-'}</td>
        </tr>
      `).join('');
    }
  } catch (err) {
    box.innerHTML = `<span style="color:var(--danger);">Error: ${err.message}</span>`;
  }
}

async function ejecutarTransicionInforme(accion) {
  if (!currentGestionInformeId) return;

  try {
    let url = `${API_EVAL}/informes/${currentGestionInformeId}/${accion}`;
    let method = 'POST';
    let body = null;

    if (accion === 'visar') {
      body = JSON.stringify({ visado: true, observacionesVisado: 'Informe visado conforme por el revisor.' });
    }

    const res = await fetchConAuth(url, {
      method: method,
      headers: body ? { 'Content-Type': 'application/json' } : {},
      body: body
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Error en la acción ${accion}`);
    }

    alert(`✅ Operación '${accion}' realizada exitosamente.`);
    verGestionInforme(document.getElementById('gestionExpedienteId').value, currentGestionInformeId);
    cargarBandejaEvaluacion();
  } catch (err) {
    alert(`❌ Error al ejecutar acción:\n${err.message}`);
  }
}

async function abrirSubModalDevolver() {
  const motivo = prompt('Ingrese el motivo de la devolución u observación:');
  if (!motivo || !motivo.trim()) return;

  try {
    const res = await fetchConAuth(`${API_EVAL}/informes/${currentGestionInformeId}/devolver`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ motivoDevolucion: motivo.trim() })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al devolver informe');
    }

    alert('✅ Informe devuelto y puesto en estado OBSERVADO.');
    verGestionInforme(document.getElementById('gestionExpedienteId').value, currentGestionInformeId);
    cargarBandejaEvaluacion();
  } catch (err) {
    alert(`❌ ${err.message}`);
  }
}

async function abrirSubModalAprobar() {
  const obs = prompt('Ingrese observaciones de aprobación final de gerencia (opcional):', 'Aprobado formalmente conforme a los recaudos y normativa TUPA.');
  if (obs === null) return;

  try {
    const res = await fetchConAuth(`${API_EVAL}/informes/${currentGestionInformeId}/aprobar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ observacionesAprobacion: obs.trim() })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al aprobar informe');
    }

    alert('✅ Informe APROBADO por funcionario competente. El expediente ha pasado al estado APTO_PARA_RESOLUCION.');
    verGestionInforme(document.getElementById('gestionExpedienteId').value, currentGestionInformeId);
    cargarBandejaEvaluacion();
    cargarExpedientes();
  } catch (err) {
    alert(`❌ No se pudo aprobar el informe:\n${err.message}`);
  }
}

async function descargarInformePreliminarPdf() {
  if (!currentGestionInformeId) return;

  try {
    const res = await fetchConAuth(`${API_EVAL}/informes/${currentGestionInformeId}/pdf-preliminar`);
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al descargar el PDF preliminar');
    }

    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `INFORME_PRELIMINAR_${currentGestionInformeId.substring(0,8)}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    alert(`❌ Error al descargar PDF preliminar:\n${err.message}`);
  }
}

/* ══════════════════════════════════════════════════════════════════════════
   M09: RESOLUCIONES ADMINISTRATIVAS Y LICENCIAS OFICIALES (I08)
   ══════════════════════════════════════════════════════════════════════════ */

const API_RESOLUCIONES = '/api/resoluciones';
const API_LICENCIAS    = '/api/licencias';
let currentGestionResolucionId = null;
let currentGestionResolucionExpedienteId = null;
let currentLicenciaEmitidaId = null;

function abrirBandejaResoluciones() {
  const modal = document.getElementById('modalBandejaResolucionesI08');
  if (modal) modal.style.display = 'flex';
  cargarBandejaResoluciones();
}

async function cargarBandejaResoluciones() {
  const tbody = document.getElementById('tablaBandejaResolucionesBody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="8" style="text-align: center;">Cargando bandeja de resoluciones...</td></tr>';

  try {
    // Obtenemos expedientes para evaluar resoluciones
    const res = await fetchConAuth(`${API_BASE}?size=50`);
    if (!res.ok) throw new Error('Error al cargar expedientes');

    const expedientes = await res.json();
    const filtro = document.getElementById('filtroResolucionesBusqueda')?.value?.trim().toLowerCase();

    // Filtrar expedientes relevantes para resolución (APTO_PARA_RESOLUCION, APROBADO) o coincidentes con búsqueda
    let lista = expedientes.filter(e => {
      if (filtro) {
        const txt = `${e.numeroTramite} ${e.nombreTitular} ${e.nombreComercial || ''}`.toLowerCase();
        return txt.includes(filtro);
      }
      return e.estado === 'APTO_PARA_RESOLUCION' || e.estado === 'APROBADO';
    });

    if (lista.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted);">No se encontraron expedientes aptos para resolución gerencial.</td></tr>';
      return;
    }

    // Para cada expediente, verificar si ya tiene resolución
    const filasHtml = await Promise.all(lista.map(async exp => {
      let resData = null;
      try {
        const r = await fetchConAuth(`${API_RESOLUCIONES}/expediente/${exp.id}`);
        if (r.ok) resData = await r.json();
      } catch (_) {}

      let licData = null;
      if (exp.estado === 'APROBADO' || (resData && resData.estadoResolucion === 'FIRMADA')) {
        try {
          const l = await fetchConAuth(`${API_LICENCIAS}/expediente/${exp.id}`);
          if (l.ok) licData = await l.json();
        } catch (_) {}
      }

      const estadoRes = resData ? resData.estadoResolucion : 'NO_GENERADA';
      let badgeRes = '<span class="status-badge" style="background:#f1f5f9; color:#475569;">SIN RESOLUCIÓN</span>';
      if (estadoRes === 'BORRADOR') {
        badgeRes = '<span class="status-badge" style="background:#fef3c7; color:#92400e;">BORRADOR</span>';
      } else if (estadoRes === 'APROBADA_PENDIENTE_FIRMA') {
        badgeRes = '<span class="status-badge" style="background:#e0e7ff; color:#3730a3;">APROBADA PEND. FIRMA</span>';
      } else if (estadoRes === 'FIRMADA') {
        badgeRes = '<span class="status-badge" style="background:#d1fae5; color:#065f46;">FIRMADA DIGITAL</span>';
      } else if (estadoRes === 'ERROR_FIRMA') {
        badgeRes = '<span class="status-badge" style="background:#fee2e2; color:#991b1b;">ERROR FIRMA</span>';
      }

      let badgeLic = licData
        ? `<span class="status-badge" style="background:#dcfce7; color:#15803d;">EMITIDA (${licData.numeroLicencia})</span>`
        : '<span class="status-badge" style="background:#f8fafc; color:#94a3b8;">NO EMITIDA</span>';

      let acciones = '';
      if (!resData) {
        if (exp.estado === 'APTO_PARA_RESOLUCION') {
          acciones += `<button class="btn btn-primary btn-sm" onclick="abrirModalPrepararResolucion('${exp.id}', '${exp.numeroTramite}', '${exp.nombreTitular.replace(/'/g, "\\'")}')">📝 Formular Resolución</button>`;
        } else {
          acciones += `<span class="hint" style="color:var(--text-muted);">Requiere Informe Aprobado</span>`;
        }
      } else {
        acciones += `<button class="btn btn-secondary btn-sm" onclick="verGestionResolucion('${resData.id}', '${exp.id}')">⚖️ Gestionar (${resData.numeroResolucion})</button>`;
      }

      return `
        <tr>
          <td><strong>${exp.numeroTramite}</strong></td>
          <td>${exp.nombreTitular}<br><span class="hint">${exp.nombreComercial || '-'}</span></td>
          <td><span class="status-badge" style="background:#e2e8f0; color:#334155;">INF CONFORME</span></td>
          <td><span class="status-badge">${exp.estado}</span></td>
          <td>${resData ? resData.numeroResolucion : '-'}</td>
          <td>${badgeRes}</td>
          <td>${badgeLic}</td>
          <td><div style="display:flex; gap:0.25rem; flex-wrap:wrap;">${acciones}</div></td>
        </tr>
      `;
    }));

    tbody.innerHTML = filasHtml.join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--danger);">Error al cargar bandeja: ${err.message}</td></tr>`;
  }
}

function abrirModalPrepararResolucion(expedienteId, numeroTramite, titular) {
  document.getElementById('resolucionExpedienteId').value = expedienteId;
  document.getElementById('txtFundamentosResolucion').value = `Que, de la evaluación técnica e integral del expediente tramitado por don(ña) ${titular}, se verificó el cumplimiento de las condiciones de zonificación, comprobante SAT validado y condiciones de seguridad en edificaciones (ITSE).`;
  document.getElementById('txtReferenciasLegalesResolucion').value = 'Ley N° 27972 Orgánica de Municipalidades, Ley N° 28976 Ley Marco de Licencias de Funcionamiento, TUO de la Ley N° 27444.';
  document.getElementById('txtParteResolutiva').value = '';
  document.getElementById('modalPrepararResolucionI08').style.display = 'flex';
}

async function guardarBorradorResolucion() {
  const expedienteId = document.getElementById('resolucionExpedienteId').value;
  const tipoDecision = document.getElementById('selectTipoDecisionResolucion').value;
  const fundamentos = document.getElementById('txtFundamentosResolucion').value.trim();
  const referenciasLegales = document.getElementById('txtReferenciasLegalesResolucion').value.trim();
  const parteResolutiva = document.getElementById('txtParteResolutiva').value.trim();

  if (!fundamentos || fundamentos.length < 20) {
    alert('Los fundamentos son obligatorios y deben contener al menos 20 caracteres.');
    return;
  }

  try {
    const res = await fetchConAuth(API_RESOLUCIONES, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        expedienteId,
        tipoDecision,
        fundamentos,
        referenciasLegales,
        parteResolutiva: parteResolutiva || null
      })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al guardar borrador de resolución');
    }

    const data = await res.json();
    alert(`✅ Borrador de Resolución creado exitosamente con número: ${data.numeroResolucion}`);
    cerrarModal('modalPrepararResolucionI08');
    cargarBandejaResoluciones();
    verGestionResolucion(data.id, expedienteId);
  } catch (err) {
    alert(`❌ No se pudo crear la resolución:\n${err.message}`);
  }
}

async function verGestionResolucion(resolucionId, expedienteId) {
  currentGestionResolucionId = resolucionId;
  currentGestionResolucionExpedienteId = expedienteId;
  document.getElementById('gestionResolucionId').value = resolucionId;
  document.getElementById('gestionResolucionExpedienteId').value = expedienteId;

  const box = document.getElementById('detallesResolucionBox');
  const tbodyHist = document.getElementById('tablaHistorialResolucionBody');
  const modal = document.getElementById('modalGestionResolucionI08');
  if (modal) modal.style.display = 'flex';

  box.innerHTML = '<p>Cargando datos de la resolución...</p>';
  tbodyHist.innerHTML = '';

  try {
    const res = await fetchConAuth(`${API_RESOLUCIONES}/${resolucionId}`);
    if (!res.ok) throw new Error('Error al obtener resolución');
    const r = await res.json();

    let lic = null;
    try {
      const lRes = await fetchConAuth(`${API_LICENCIAS}/expediente/${r.expedienteId}`);
      if (lRes.ok) lic = await lRes.json();
    } catch (_) {}

    currentLicenciaEmitidaId = lic ? lic.id : null;

    box.innerHTML = `
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.75rem;">
        <div><strong>Resolución:</strong> ${r.numeroResolucion} (v${r.version})</div>
        <div><strong>Expediente N°:</strong> ${r.numeroExpediente}</div>
        <div><strong>Sentido Decisión:</strong> <span class="status-badge" style="background:#e0f2fe; color:#0369a1;">${r.tipoDecision}</span></div>
        <div><strong>Estado Resolución:</strong> <span class="status-badge" style="background:#f1f5f9; color:#0f172a;">${r.estadoResolucion}</span></div>
        <div><strong>Autoridad Aprobador:</strong> ${r.autoridadCompetenteNombre || 'Pendiente'} (${r.autoridadCompetenteCargo || '-'})</div>
        <div><strong>Fecha Aprobación:</strong> ${r.fechaAprobacion ? r.fechaAprobacion.replace('T', ' ') : 'Pendiente'}</div>
        <div><strong>Firmante Digital:</strong> ${r.firmanteNombre || 'Pendiente de firma PKI'}</div>
        <div><strong>Hash SHA-256:</strong> <code style="font-size:0.75rem;">${r.hashDocumento ? r.hashDocumento.substring(0,24) + '...' : 'N/D'}</code></div>
      </div>
      ${lic ? `<div style="margin-top:0.75rem; padding:0.5rem; background:#ecfdf5; border:1px solid #a7f3d0; border-radius:6px; font-weight:600; color:#065f46;">
        🏷️ Licencia Oficial Emitida: ${lic.numeroLicencia} | QR: ${lic.codigoVerificacionQr.substring(0,12)}... | Vigencia: ${lic.tipoVigencia}
      </div>` : ''}
    `;

    // Botones de acción según estado
    const btnAprobar = document.getElementById('btnAccionAprobarResolucion');
    const btnFirmar = document.getElementById('btnAccionFirmarResolucion');
    const btnEmitirLic = document.getElementById('btnAccionEmitirLicencia');
    const btnDescargarLic = document.getElementById('btnAccionDescargarLicencia');
    const alertaFirmaBox = document.getElementById('firmaDigitalAlertaBox');

    btnAprobar.style.display = (r.estadoResolucion === 'BORRADOR') ? 'inline-block' : 'none';
    btnFirmar.style.display = (r.estadoResolucion === 'APROBADA_PENDIENTE_FIRMA' || r.estadoResolucion === 'ERROR_FIRMA') ? 'inline-block' : 'none';
    btnEmitirLic.style.display = (r.estadoResolucion === 'FIRMADA' && r.tipoDecision === 'FAVORABLE' && !lic) ? 'inline-block' : 'none';
    btnDescargarLic.style.display = (lic) ? 'inline-block' : 'none';

    // Alerta de firma digital
    if (r.estadoResolucion === 'APROBADA_PENDIENTE_FIRMA' || r.estadoResolucion === 'ERROR_FIRMA') {
      alertaFirmaBox.style.display = 'block';
      alertaFirmaBox.style.background = '#fef3c7';
      alertaFirmaBox.style.color = '#92400e';
      alertaFirmaBox.innerHTML = '⚠️ <strong>Salvaguarda Criptográfica (Ley N° 27269):</strong> La resolución se encuentra aprobada administrativamente pero requiere la firma digital de la autoridad mediante el proveedor institucional PKI. Si el servicio no está configurado, la emisión oficial de licencia queda suspendida por mandato legal.';
    } else if (r.estadoResolucion === 'FIRMADA') {
      alertaFirmaBox.style.display = 'block';
      alertaFirmaBox.style.background = '#d1fae5';
      alertaFirmaBox.style.color = '#065f46';
      alertaFirmaBox.innerHTML = '✅ <strong>Firma Digital Verificada:</strong> El documento cuenta con firma digital institucional y hash criptográfico auditado. Habilitado para la emisión de la Licencia Oficial de Funcionamiento.';
    } else {
      alertaFirmaBox.style.display = 'none';
    }

    // Historial
    const hist = r.historial || [];
    if (hist.length === 0) {
      tbodyHist.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">Sin actuaciones registradas.</td></tr>';
    } else {
      tbodyHist.innerHTML = hist.map(h => `
        <tr>
          <td>${h.fechaRegistro ? h.fechaRegistro.replace('T', ' ') : '-'}</td>
          <td><strong>${h.accion}</strong></td>
          <td><span class="status-badge">${h.estadoNuevo}</span></td>
          <td>${h.actor}</td>
          <td>${h.motivoObservaciones || '-'}</td>
        </tr>
      `).join('');
    }

  } catch (err) {
    box.innerHTML = `<p style="color:var(--danger);">Error al cargar resolución: ${err.message}</p>`;
  }
}

async function aprobarResolucionGerencial() {
  if (!currentGestionResolucionId) return;
  const obs = prompt('Ingrese observaciones de aprobación gerencial (opcional):', 'Aprobado formalmente conforme a los recaudos y normativa TUPA.');
  if (obs === null) return;

  try {
    const res = await fetchConAuth(`${API_RESOLUCIONES}/${currentGestionResolucionId}/aprobar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        observaciones: obs.trim(),
        cargoAutoridad: 'Gerente de Desarrollo Económico'
      })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al aprobar resolución');
    }

    alert('✅ Resolución APROBADA formalmente. Ha pasado al estado APROBADA_PENDIENTE_FIRMA.');
    verGestionResolucion(currentGestionResolucionId, currentGestionResolucionExpedienteId);
    cargarBandejaResoluciones();
  } catch (err) {
    alert(`❌ No se pudo aprobar la resolución:\n${err.message}`);
  }
}

async function solicitarFirmaDigital() {
  if (!currentGestionResolucionId) return;
  if (!confirm('¿Desea enviar la resolución al servicio institucional de firma digital PKI?')) return;

  try {
    const res = await fetchConAuth(`${API_RESOLUCIONES}/${currentGestionResolucionId}/firmar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ motivoFirma: 'Resolución de Licencia de Funcionamiento' })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al solicitar firma digital');
    }

    alert('✅ Firma Digital institucional aplicada exitosamente.');
    verGestionResolucion(currentGestionResolucionId, currentGestionResolucionExpedienteId);
    cargarBandejaResoluciones();
  } catch (err) {
    alert(`⚠️ Resultado de Firma Digital:\n${err.message}`);
    verGestionResolucion(currentGestionResolucionId, currentGestionResolucionExpedienteId);
  }
}

async function descargarResolucionPdf() {
  if (!currentGestionResolucionId) return;

  try {
    const res = await fetchConAuth(`${API_RESOLUCIONES}/${currentGestionResolucionId}/pdf`);
    if (!res.ok) throw new Error('Error al descargar el PDF de la resolución');

    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `RESOLUCION_${currentGestionResolucionId.substring(0,8)}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    alert(`❌ Error al descargar PDF de resolución:\n${err.message}`);
  }
}

async function abrirSubModalEmitirLicencia() {
  if (!currentGestionResolucionId) return;
  if (!confirm('¿Confirma la emisión definitiva del Certificado Oficial de Licencia de Funcionamiento con código QR inmutable?')) return;

  try {
    const res = await fetchConAuth(`${API_LICENCIAS}/emitir`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        resolucionId: currentGestionResolucionId,
        observacionesEmision: 'Licencia emitida formalmente en cumplimiento del acto resolutivo.'
      })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Error al emitir la licencia');
    }

    const data = await res.json();
    alert(`🎉 ¡Licencia de Funcionamiento N° ${data.numeroLicencia} emitida exitosamente!\nExpediente APROBADO y código QR generado: ${data.codigoVerificacionQr.substring(0,12)}...`);
    verGestionResolucion(currentGestionResolucionId, currentGestionResolucionExpedienteId);
    cargarBandejaResoluciones();
    cargarExpedientes();
  } catch (err) {
    alert(`❌ No se pudo emitir la licencia:\n${err.message}`);
  }
}

async function descargarLicenciaOficialPdf() {
  if (!currentLicenciaEmitidaId) return;

  try {
    const res = await fetchConAuth(`${API_LICENCIAS}/${currentLicenciaEmitidaId}/pdf`);
    if (!res.ok) throw new Error('Error al descargar el certificado oficial de licencia');

    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `LICENCIA_OFICIAL_${currentLicenciaEmitidaId.substring(0,8)}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    alert(`❌ Error al descargar certificado de licencia:\n${err.message}`);
  }
}
