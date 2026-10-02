const API_BASE        = '/api/expedientes';
const API_FORMULARIOS = '/api/formularios';
const API_AUTH        = '/api/auth';

document.addEventListener('DOMContentLoaded', () => {
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
  const modalLogin = document.getElementById('modalLogin');

  if (token && usuario) {
    const rolCorto = rol ? rol.replace('ROLE_', '') : 'USUARIO';
    label.innerHTML = `👤 <strong>${usuario}</strong> <span style="font-size: 0.75rem; background: rgba(255,255,255,0.25); padding: 0.15rem 0.4rem; border-radius: 4px; margin-left: 0.25rem;">${rolCorto}</span>`;
    label.style.display = 'inline-block';
    btnCerrar.style.display = 'inline-block';
    if (modalLogin) modalLogin.style.display = 'none';
  } else {
    label.style.display = 'none';
    btnCerrar.style.display = 'none';
    if (modalLogin) modalLogin.style.display = 'flex';
  }
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
  const tbody = document.getElementById('tablaExpedientesBody');
  if (tbody) {
    tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted);">Inicie sesión para acceder a la bandeja de trámites.</td></tr>';
  }
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
    cerrarSesion();
    throw new Error('Su sesión ha expirado o no está autorizado. Inicie sesión nuevamente.');
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
      <a href="${API_FORMULARIOS}/anexo-3/${exp.id}/pdf" target="_blank" class="btn btn-secondary btn-sm" title="Descargar Anexo 3 - Reporte de Riesgo ITSE">
        🛡️ Anexo 3
      </a>
      <a href="${API_FORMULARIOS}/anexo-4/${exp.id}/pdf" target="_blank" class="btn btn-secondary btn-sm" title="Descargar Anexo 4 - Declaración de Condiciones de Seguridad">
        📋 Anexo 4
      </a>
    `;
  }

  if (exp.voucherSatId) {
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
    `;
  }

  if (exp.estado === 'DOCUMENTOS_VALIDADOS') {
    return `
      <button class="btn btn-secondary btn-sm" onclick="generarVoucherSat('${exp.id}')" title="Generar código de voucher SAT">
        🧾 Generar Voucher
      </button>
      <button class="btn btn-success btn-sm" onclick="abrirModalPago('${exp.id}', '${exp.voucherSatId || ''}')">
        💳 Validar Pago
      </button>
      <button class="btn btn-danger btn-sm" onclick="abrirModalRechazo('${exp.id}')">
        ❌ Rechazar
      </button>
    `;
  }

  if (exp.estado === 'EN_EVALUACION_FINAL') {
    return `
      <button class="btn btn-primary btn-sm" onclick="aprobarExpediente('${exp.id}')">
        ✅ Emitir Licencia (QR)
      </button>
      <button class="btn btn-danger btn-sm" onclick="abrirModalRechazo('${exp.id}')">
        ❌ Rechazar
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
    const res = await fetchConAuth(`${API_BASE}/${id}/voucher`);
    if (!res.ok) throw new Error('Error al generar voucher');
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
