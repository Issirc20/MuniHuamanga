document.addEventListener('DOMContentLoaded', () => {
  const inputCodigo = document.getElementById('inputCodigo');
  const btnConsultar = document.getElementById('btnConsultar');
  const loadingBox = document.getElementById('loadingBox');
  const resultadoBox = document.getElementById('resultadoBox');

  // Leer parámetro ?codigo= de la URL
  const urlParams = new URLSearchParams(window.location.search);
  const codigoFromUrl = urlParams.get('codigo');

  if (codigoFromUrl) {
    inputCodigo.value = codigoFromUrl.trim();
    consultarLicencia(codigoFromUrl.trim());
  }

  btnConsultar.addEventListener('click', () => {
    const val = inputCodigo.value.trim();
    if (!val) {
      alert('Por favor ingrese un código de licencia (ej: LIC-2026-XXXXXXXX).');
      return;
    }
    consultarLicencia(val);
  });

  inputCodigo.addEventListener('keyup', (e) => {
    if (e.key === 'Enter') {
      btnConsultar.click();
    }
  });

  async function consultarLicencia(codigo) {
    loadingBox.style.display = 'block';
    resultadoBox.style.display = 'none';

    try {
      const resp = await fetch(`/api/public/licencias/${encodeURIComponent(codigo)}`);
      if (!resp.ok) {
        throw new Error(`Error en el servidor al consultar la licencia: ${resp.status}`);
      }
      const data = await resp.json();
      renderResultado(data);
    } catch (err) {
      console.error(err);
      renderError(codigo, err.message);
    } finally {
      loadingBox.style.display = 'none';
      resultadoBox.style.display = 'block';
    }
  }

  function renderResultado(lic) {
    if (lic.valida) {
      resultadoBox.innerHTML = `
        <div class="verify-card">
          <div class="verify-header">
            <span style="font-size: 0.85rem; letter-spacing: 1px; text-transform: uppercase; opacity: 0.9;">
              Constatación de Autenticidad en Tiempo Real
            </span>
            <h2 style="font-size: 1.5rem; margin: 0.5rem 0; font-weight: 800;">
              MUNICIPALIDAD PROVINCIAL DE HUAMANGA
            </h2>
            <div class="verify-badge-valid">
              ✓ LICENCIA OFICIAL Y VIGENTE
            </div>
          </div>

          <div class="verify-body">
            <div style="background: #ecfdf5; border-left: 4px solid var(--success); padding: 1rem; border-radius: 6px; margin-bottom: 1.5rem;">
              <p style="color: #065f46; font-size: 0.95rem; font-weight: 500;">
                ${lic.mensajeVerificacion || 'Licencia expedida conforme a la Ley N° 28976 y TUO D.S. N° 046-2017-PCM.'}
              </p>
            </div>

            <h3 style="color: var(--primary); font-size: 1.1rem; border-bottom: 2px solid var(--border); padding-bottom: 0.5rem; margin-bottom: 1rem;">
              Datos del Establecimiento Autorizado
            </h3>

            <div class="data-grid">
              <div class="data-item">
                <div class="data-label">Código de Licencia</div>
                <div class="data-val" style="color: var(--primary);">${lic.numeroLicencia || '-'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Número de Expediente</div>
                <div class="data-val">${lic.numeroTramite || '-'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Titular o Solicitante</div>
                <div class="data-val">${lic.titular || '-'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Documento de Identidad</div>
                <div class="data-val">${lic.documentoIdentidad || '-'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Razón Social</div>
                <div class="data-val">${lic.razonSocial || '(Persona Natural)'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Nombre Comercial</div>
                <div class="data-val">${lic.nombreComercial || '-'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Giro o Actividad Autorizada</div>
                <div class="data-val">${lic.giro || '-'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Dirección en Huamanga</div>
                <div class="data-val">${lic.direccion || '-'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Nivel de Riesgo ITSE</div>
                <div class="data-val">${lic.nivelRiesgo || '-'}</div>
              </div>
              <div class="data-item">
                <div class="data-label">Vigencia Legal</div>
                <div class="data-val" style="color: var(--success);">INDETERMINADA (Art. 11 Ley 28976)</div>
              </div>
            </div>

            <div class="qr-security-box">
              <img class="qr-preview-img" src="/api/public/licencias/${encodeURIComponent(lic.numeroLicencia)}/qr?size=160" alt="Código QR Licencia" width="160" height="160" />
              <div>
                <p style="font-weight: 700; color: var(--primary); font-size: 0.95rem;">Código QR Criptográfico Certificado</p>
                <p style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.25rem;">
                  Firma Electrónica Avanzada: Abog. Carlos Mendoza Palomino (Gerente de Licencias)
                </p>
              </div>
            </div>

            <p class="legal-notice">
              Este registro digital es público y de libre consulta en cumplimiento del Principio de Publicidad y Transparencia Administrativa (Ley N° 27444).
            </p>
          </div>
        </div>
      `;
    } else {
      resultadoBox.innerHTML = `
        <div class="verify-card">
          <div class="verify-header" style="background: linear-gradient(135deg, #7f1d1d 0%, #b91c1c 100%);">
            <span style="font-size: 0.85rem; letter-spacing: 1px; text-transform: uppercase; opacity: 0.9;">
              Constatación de Autenticidad
            </span>
            <h2 style="font-size: 1.5rem; margin: 0.5rem 0; font-weight: 800;">
              MUNICIPALIDAD PROVINCIAL DE HUAMANGA
            </h2>
            <div class="verify-badge-invalid">
              ✕ LICENCIA NO REGISTRADA O NO VÁLIDA
            </div>
          </div>

          <div class="verify-body">
            <div style="background: #fef2f2; border-left: 4px solid var(--danger); padding: 1rem; border-radius: 6px; margin-bottom: 1.5rem;">
              <p style="color: #991b1b; font-size: 0.95rem; font-weight: 500;">
                ${lic.mensajeVerificacion || 'El código de licencia ingresado no corresponde a una licencia vigente en el padrón municipal.'}
              </p>
            </div>

            <div class="data-item" style="border-left-color: var(--danger); margin-bottom: 1.5rem;">
              <div class="data-label">Código Consultado</div>
              <div class="data-val" style="color: var(--danger);">${lic.numeroLicencia || inputCodigo.value}</div>
            </div>

            <p style="font-size: 0.9rem; color: var(--text-muted); line-height: 1.5;">
              Si usted es el titular y considera que esto es un error, por favor verifique que el número de trámite haya completado la fase de evaluación final y cuente con resolución aprobatoria de la Gerencia de Licencias.
            </p>
          </div>
        </div>
      `;
    }
  }

  function renderError(codigo, errorMsg) {
    resultadoBox.innerHTML = `
      <div class="verify-card">
        <div class="verify-header" style="background: var(--danger);">
          <h2>Error de Conexión</h2>
        </div>
        <div class="verify-body" style="text-align: center;">
          <p style="color: var(--danger); font-weight: 600; margin-bottom: 1rem;">
            No se pudo verificar el código: ${codigo}
          </p>
          <p style="font-size: 0.9rem; color: var(--text-muted);">${errorMsg}</p>
        </div>
      </div>
    `;
  }
});
