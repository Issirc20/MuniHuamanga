const API_BASE = '/api/expedientes';

document.addEventListener('DOMContentLoaded', () => {
  cargarExpedientes();
});

async function cargarExpedientes(soloAlerta = false) {
  const tbody = document.getElementById('tablaExpedientesBody');
  tbody.innerHTML = '<tr><td colspan="8">Cargando trámites...</td></tr>';

  const estadoFiltro = document.getElementById('filtroEstado').value;
  let url = API_BASE;
  const params = [];
  if (estadoFiltro) params.push(`estado=${estadoFiltro}`);
  if (soloAlerta) params.push(`conAlerta=true`);
  if (params.length > 0) url += '?' + params.join('&');

  try {
    const res = await fetch(url);
    const expedientes = await res.json();

    actualizarKpis(expedientes);

    if (!expedientes || expedientes.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted);">No se encontraron expedientes.</td></tr>';
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
              ${renderAcciones(exp)}
              <a class="btn btn-secondary btn-sm" href="${API_BASE}/${exp.id}/documentos/declaracion-jurada" target="_blank" title="Descargar Declaración Jurada">📄 DJ</a>
              ${exp.montoTasa ? `<a class="btn btn-secondary btn-sm" href="${API_BASE}/${exp.id}/documentos/voucher-sat" target="_blank" title="Descargar Voucher SAT">🧾 Voucher</a>` : ''}
              <button class="btn btn-secondary btn-sm" onclick="verHistorial('${exp.id}')">📜 Trazabilidad</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" style="color: var(--danger);">Error al cargar expedientes: ${err.message}</td></tr>`;
  }
}

function renderAcciones(exp) {
  if (exp.estado === 'FORMATOS_GENERADOS') {
    return `<button class="btn btn-primary btn-sm" onclick="abrirModalItse('${exp.id}')">🛡️ Dictamen ITSE</button>`;
  }
  if (exp.estado === 'DOCUMENTOS_VALIDADOS') {
    return `
      <button class="btn btn-secondary btn-sm" onclick="generarVoucherSat('${exp.id}')">🧾 Voucher SAT</button>
      <button class="btn btn-success btn-sm" onclick="abrirModalPago('${exp.id}', '${exp.voucherId || ''}')">💳 Pago SAT</button>
    `;
  }
  if (exp.estado === 'EN_EVALUACION_FINAL') {
    return `
      <button class="btn btn-success btn-sm" onclick="aprobarExpediente('${exp.id}')">✅ Aprobar (QR)</button>
      <button class="btn btn-danger btn-sm" onclick="abrirModalRechazo('${exp.id}')">❌ Rechazar</button>
    `;
  }
  if (exp.estado === 'APROBADO') {
    return `<span class="badge badge-aprobado">QR: ${exp.licenciaQrCode || 'EMITIDA'}</span>`;
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

/* Modales y Operaciones */
function abrirModalItse(id) {
  document.getElementById('itseExpedienteId').value = id;
  document.getElementById('modalItse').style.display = 'flex';
}

async function guardarClasificacionItse() {
  const id = document.getElementById('itseExpedienteId').value;
  const nivel = document.getElementById('selectNivelRiesgo').value;
  const informe = document.getElementById('txtInformeItse').value;
  const obs = document.getElementById('txtObsItse').value;

  try {
    const res = await fetch(`${API_BASE}/${id}/clasificacion-riesgo`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nivelRiesgo: nivel, informeItseNumero: informe, observaciones: obs, usuario: 'DEFENSA_CIVIL' })
    });
    if (!res.ok) throw new Error('Error al registrar dictamen ITSE');
    cerrarModal('modalItse');
    cargarExpedientes();
  } catch (err) {
    alert(err.message);
  }
}

async function generarVoucherSat(id) {
  try {
    const res = await fetch(`${API_BASE}/${id}/voucher`, { method: 'POST' });
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

  try {
    const res = await fetch(`${API_BASE}/${id}/pago`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ voucherId: voucher, numeroOperacionSat: op, montoPagado: 218.00, usuario: 'SAT_CAJA' })
    });
    if (!res.ok) throw new Error('Error al registrar pago');
    cerrarModal('modalPago');
    cargarExpedientes();
  } catch (err) {
    alert(err.message);
  }
}

async function aprobarExpediente(id) {
  if (!confirm('¿Confirma la APROBACIÓN del expediente y la emisión de la Licencia Digital con Código QR?')) return;

  try {
    const res = await fetch(`${API_BASE}/${id}/aprobar`, { method: 'POST' });
    if (!res.ok) {
      const err = await res.json();
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

  try {
    const res = await fetch(`${API_BASE}/${id}/rechazar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ aprobado: false, motivo: motivo, funcionarioResponsable: 'GERENCIA_LICENCIAS' })
    });
    if (!res.ok) throw new Error('Error al rechazar expediente');
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
    const res = await fetch(`${API_BASE}/${id}/historial`);
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
    tbody.innerHTML = '<tr><td colspan="4">Error al cargar historial.</td></tr>';
  }
}

function cerrarModal(modalId) {
  document.getElementById(modalId).style.display = 'none';
}
