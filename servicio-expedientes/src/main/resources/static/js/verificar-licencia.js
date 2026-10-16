document.addEventListener('DOMContentLoaded', () => {
  const inputCodigo = document.getElementById('inputCodigo');
  const btnConsultar = document.getElementById('btnConsultar');
  const loadingBox = document.getElementById('loadingBox');
  const resultadoBox = document.getElementById('resultadoBox');
  const tabPublico = document.getElementById('tabPublico');
  const tabInspector = document.getElementById('tabInspector');
  const inspectorAuthAlert = document.getElementById('inspectorAuthAlert');

  let modoActual = 'PUBLICO'; // 'PUBLICO' o 'INSPECTOR'
  let leafletMap = null;
  let inspectorMarker = null;

  // Leer parámetros de la URL (?token= o ?codigo=)
  const urlParams = new URLSearchParams(window.location.search);
  const tokenFromUrl = urlParams.get('token') || urlParams.get('codigo');
  const modoFromUrl = urlParams.get('modo');

  if (modoFromUrl && modoFromUrl.toUpperCase() === 'INSPECTOR') {
    activarModoInspector();
  }

  if (tokenFromUrl) {
    inputCodigo.value = tokenFromUrl.trim();
    consultarLicencia(tokenFromUrl.trim());
  }

  tabPublico.addEventListener('click', () => {
    modoActual = 'PUBLICO';
    tabPublico.classList.add('active');
    tabInspector.classList.remove('active');
    inspectorAuthAlert.style.display = 'none';
    if (inputCodigo.value.trim()) {
      consultarLicencia(inputCodigo.value.trim());
    }
  });

  tabInspector.addEventListener('click', () => {
    activarModoInspector();
  });

  function activarModoInspector() {
    modoActual = 'INSPECTOR';
    tabInspector.classList.add('active');
    tabPublico.classList.remove('active');

    const tokenJwt = obtenerTokenJwt();
    if (!tokenJwt) {
      inspectorAuthAlert.style.display = 'flex';
    } else {
      inspectorAuthAlert.style.display = 'none';
    }

    if (inputCodigo.value.trim()) {
      consultarLicencia(inputCodigo.value.trim());
    }
  }

  function obtenerTokenJwt() {
    return localStorage.getItem('jwt_token') || localStorage.getItem('token') || sessionStorage.getItem('token') || '';
  }

  btnConsultar.addEventListener('click', () => {
    const val = inputCodigo.value.trim();
    if (!val) {
      alert('Por favor ingrese o escanee el token QR de la licencia.');
      return;
    }
    consultarLicencia(val);
  });

  inputCodigo.addEventListener('keyup', (e) => {
    if (e.key === 'Enter') {
      btnConsultar.click();
    }
  });

  async function consultarLicencia(token) {
    loadingBox.style.display = 'block';
    resultadoBox.style.display = 'none';

    try {
      if (modoActual === 'INSPECTOR') {
        const tokenJwt = obtenerTokenJwt();
        if (!tokenJwt) {
          inspectorAuthAlert.style.display = 'flex';
          throw new Error('Debe iniciar sesión como INSPECTOR o ADMINISTRADOR para consultar este módulo.');
        }

        const resp = await fetch(`/api/inspector/licencias/verificar/${encodeURIComponent(token)}`, {
          headers: {
            'Authorization': `Bearer ${tokenJwt}`
          }
        });

        if (resp.status === 401 || resp.status === 403) {
          inspectorAuthAlert.style.display = 'flex';
          throw new Error('Credenciales insuficientes: se requiere perfil ROLE_INSPECTOR o ROLE_ADMIN.');
        }

        if (!resp.ok) {
          const errData = await resp.json().catch(() => ({}));
          throw new Error(errData.mensaje || `Error en el servidor al consultar (${resp.status})`);
        }

        const data = await resp.json();
        renderResultadoInspector(data);
      } else {
        const resp = await fetch(`/api/licencias/verificar/${encodeURIComponent(token)}`);
        if (!resp.ok) {
          const errData = await resp.json().catch(() => ({}));
          throw new Error(errData.mensaje || `Código QR no encontrado en el padrón municipal (${resp.status})`);
        }
        const data = await resp.json();
        renderResultadoPublico(data);
      }
    } catch (err) {
      console.error(err);
      renderError(token, err.message);
    } finally {
      loadingBox.style.display = 'none';
      resultadoBox.style.display = 'block';
    }
  }

  function renderResultadoPublico(lic) {
    const badgeClass = lic.esSimulada ? 'badge-advertencia' : obtenerBadgeClass(lic.estadoLicencia, lic.valida);
    const badgeTexto = lic.esSimulada ? '⚠ DEMOSTRACIÓN — SIN VALIDEZ OFICIAL' : obtenerBadgeTexto(lic.estadoLicencia, lic.valida);

    resultadoBox.innerHTML = `
      <div class="verify-card">
        <div class="verify-header" style="${lic.esSimulada ? 'background: linear-gradient(135deg, #78350f 0%, #d97706 100%);' : ''}">
          <img class="escudo-muni" src="img/escudo-huamanga.png" alt="Escudo de Huamanga" onerror="this.style.display='none'">
          <h1>Municipalidad Provincial de Huamanga</h1>
          <p>Constatación Oficial de Autenticidad en Tiempo Real (M10)</p>
          <div class="status-badge ${badgeClass}">
            ${badgeTexto}
          </div>
        </div>

        <div class="verify-body">
          <div class="alert-banner ${lic.esSimulada ? 'alert-warning' : (lic.valida ? 'alert-success' : 'alert-danger')}" style="${lic.esSimulada ? 'background: #fffbeb; border: 1.5px solid #f59e0b; color: #92400e;' : ''}">
            <span>${lic.esSimulada ? '⚠️ ATENCIÓN: Este documento fue emitido en entorno de DEMOSTRACIÓN / PRUEBAS. CARECE DE VALIDEZ LEGAL OFICIAL conforme a la Ley N° 27269.' : (lic.mensajeVerificacion || 'Verificación procesada con el registro oficial de licencias.')}</span>
          </div>

          <h3 style="color: #1e3a8a; font-size: 1.1rem; border-bottom: 2px solid #e2e8f0; padding-bottom: 0.5rem; margin-bottom: 1rem;">
            Datos de la Licencia y Establecimiento
          </h3>

          <div class="data-grid">
            <div class="data-item">
              <div class="data-label">Número de Licencia</div>
              <div class="data-val" style="color: #1e3a8a;">${lic.numeroLicencia || '-'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Resolución Gerencial</div>
              <div class="data-val">${lic.numeroResolucion || '-'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Nombre Comercial</div>
              <div class="data-val">${lic.nombreComercial || '-'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Titular / Razón Social</div>
              <div class="data-val">${lic.titularNombreORazonSocial || '-'}</div>
            </div>
            <div class="data-item" style="grid-column: 1 / -1;">
              <div class="data-label">Dirección Autorizada</div>
              <div class="data-val">${lic.direccionEstablecimiento || '-'}</div>
            </div>
            <div class="data-item" style="grid-column: 1 / -1;">
              <div class="data-label">Giro o Actividad Económica</div>
              <div class="data-val">${lic.giroActividad || '-'} ${lic.ciiuCodigo ? `(CIIU: ${lic.ciiuCodigo})` : ''}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Tipo de Vigencia</div>
              <div class="data-val">${lic.tipoVigencia || 'INDETERMINADA'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Fecha de Emisión</div>
              <div class="data-val">${lic.fechaEmision || '-'}</div>
            </div>
            ${lic.fechaCaducidad ? `
            <div class="data-item">
              <div class="data-label">Fecha de Caducidad</div>
              <div class="data-val" style="color: #dc2626;">${lic.fechaCaducidad}</div>
            </div>` : ''}
            <div class="data-item">
              <div class="data-label">Autoridad Emisora</div>
              <div class="data-val">${lic.autoridadEmisora || 'Subgerencia de Comercio y Licencias'}</div>
            </div>
          </div>

          <p class="legal-notice">
            Registro público oficial conforme a la Ley Marco de Licencias de Funcionamiento (Ley N° 28976) y Ley de Protección de Datos Personales (Ley N° 29733).
            <br>Consulta realizada el: <strong>${formatearFechaHora(lic.fechaConsulta)}</strong>.
            <br>La verificación corresponde al estado registrado al momento exacto de la consulta.
          </p>
        </div>
      </div>
    `;
  }

  function renderResultadoInspector(lic) {
    const badgeClass = lic.esSimulada ? 'badge-advertencia' : obtenerBadgeClass(lic.estadoLicencia, lic.valida);
    const badgeTexto = lic.esSimulada ? '⚠ DEMOSTRACIÓN — SIN VALIDEZ OFICIAL' : obtenerBadgeTexto(lic.estadoLicencia, lic.valida);

    resultadoBox.innerHTML = `
      <div class="verify-card">
        <div class="verify-header" style="${lic.esSimulada ? 'background: linear-gradient(135deg, #78350f 0%, #d97706 100%);' : 'background: linear-gradient(135deg, #064e3b 0%, #047857 50%, #10b981 100%);'}">
          <img class="escudo-muni" src="img/escudo-huamanga.png" alt="Escudo de Huamanga" onerror="this.style.display='none'">
          <h1>Panel de Fiscalización en Campo</h1>
          <p>Módulo de Consulta Móvil para Inspectores Municipales (CA-10 / I09)</p>
          <div class="status-badge ${badgeClass}">
            ${badgeTexto}
          </div>
        </div>

        <div class="verify-body">
          <div class="alert-banner ${lic.esSimulada ? 'alert-warning' : (lic.valida ? 'alert-success' : 'alert-danger')}" style="${lic.esSimulada ? 'background: #fffbeb; border: 1.5px solid #f59e0b; color: #92400e;' : ''}">
            <span>${lic.esSimulada ? '⚠️ ATENCIÓN FISCALIZADOR: Documento emitido en entorno de PRUEBAS/DEMOSTRACIÓN. No posee eficacia legal (Ley N° 27269).' : (lic.mensajeVerificacion || 'Licencia verificada para función inspectiva.')}</span>
          </div>

          <h3 style="color: #065f46; font-size: 1.1rem; border-bottom: 2px solid #bbf7d0; padding-bottom: 0.5rem; margin-bottom: 1rem;">
            Identificación Oficial del Establecimiento
          </h3>

          <div class="data-grid">
            <div class="data-item">
              <div class="data-label">Número de Licencia</div>
              <div class="data-val" style="color: #065f46;">${lic.numeroLicencia || '-'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Expediente de Trámite</div>
              <div class="data-val">${lic.numeroExpediente || '-'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Resolución de Emisión</div>
              <div class="data-val">${lic.numeroResolucion || '-'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Nombre Comercial</div>
              <div class="data-val">${lic.nombreComercial || '-'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Titular / Representante Legal</div>
              <div class="data-val">${lic.titularNombreORazonSocial || '-'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Doc. Identidad Titular</div>
              <div class="data-val">${lic.titularTipoDocumento || 'DNI/RUC'}: ${lic.titularNumeroDocumento || '-'}</div>
            </div>
            <div class="data-item" style="grid-column: 1 / -1;">
              <div class="data-label">Dirección Autorizada</div>
              <div class="data-val">${lic.direccionAutorizada || '-'}</div>
            </div>
            ${lic.referenciaUbicacion ? `
            <div class="data-item" style="grid-column: 1 / -1;">
              <div class="data-label">Referencia Municipal</div>
              <div class="data-val">${lic.referenciaUbicacion}</div>
            </div>` : ''}
            <div class="data-item" style="grid-column: 1 / -1;">
              <div class="data-label">Giro Autorizado</div>
              <div class="data-val">${lic.giroActividad || '-'} ${lic.ciiuCodigo ? `(CIIU: ${lic.ciiuCodigo})` : ''}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Aforo Permitido</div>
              <div class="data-val">${lic.aforoPersonas != null ? lic.aforoPersonas + ' personas' : 'No especificado'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Área del Local</div>
              <div class="data-val">${lic.areaMetrosCuadrados != null ? lic.areaMetrosCuadrados + ' m²' : 'No especificada'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Zonificación PDU</div>
              <div class="data-val">${lic.zonificacion || 'Comercial'}</div>
            </div>
            <div class="data-item">
              <div class="data-label">Riesgo ITSE</div>
              <div class="data-val">${lic.nivelRiesgoItse || 'MEDIO'}</div>
            </div>
          </div>

          <!-- Sección de Geolocalización y Mapa -->
          <div class="inspector-panel">
            <div class="inspector-header">
              <h3>📍 Georreferenciación Oficial del Establecimiento</h3>
              <span style="font-size: 0.8rem; font-weight: 700; color: #166534; background: #dcfce7; padding: 4px 8px; border-radius: 4px;">
                ${lic.tipoCoordenada === 'RECTIFICADA_APROBADA' ? '✓ RECTIFICADA Y APROBADA' : 'OFICIAL DECLARADA'}
              </span>
            </div>

            ${lic.coordenadasVerificadas && lic.latitud && lic.longitud ? `
              <p style="font-size: 0.9rem; color: #166534; margin: 0 0 0.5rem 0;">
                Coordenadas: <strong>Lat: ${lic.latitud}, Lon: ${lic.longitud}</strong>
              </p>
              <div id="mapContainer"></div>

              <div class="map-actions">
                ${lic.enlaceNavegacion ? `
                <a href="${lic.enlaceNavegacion}" target="_blank" rel="noopener noreferrer" class="btn-action btn-navigate">
                  🧭 Abrir en GPS (Google Maps / Waze)
                </a>` : ''}
                <button id="btnGpsInspector" class="btn-action btn-gps">
                  📍 Cotejar con mi ubicación actual (GPS)
                </button>
              </div>

              <div id="gpsResultBox" style="display: none; margin-top: 1rem; padding: 0.75rem; background: #ffffff; border-radius: 6px; border: 1px solid #cbd5e1; font-size: 0.85rem;">
              </div>
            ` : `
              <div class="alert-banner alert-warning" style="margin-top: 0.5rem;">
                <span>⚠️ Aviso: El establecimiento no cuenta con coordenadas geográficas verificadas en el padrón municipal.</span>
              </div>
            `}
          </div>

          <p class="legal-notice">
            Fiscalización oficial realizada por el inspector: <strong>${lic.inspectorConsultante || 'INSPECTOR'}</strong>
            <br>Registro de auditoría grabado el: <strong>${formatearFechaHora(lic.fechaConsulta)}</strong>.
            <br>El inspector actúa con facultades de verificación de campo; no tiene competencia para emitir, anular ni modificar títulos habilitantes.
          </p>
        </div>
      </div>
    `;

    // Si existen coordenadas, inicializar el mapa Leaflet
    if (lic.coordenadasVerificadas && lic.latitud && lic.longitud) {
      setTimeout(() => {
        inicializarMapa(Number(lic.latitud), Number(lic.longitud), lic.nombreComercial, lic.direccionAutorizada, lic.tipoCoordenada);
      }, 100);
    }
  }

  function inicializarMapa(lat, lon, nombreComercial, direccion, tipoCoordenada) {
    if (typeof L === 'undefined') {
      console.warn('Leaflet no está disponible en este entorno.');
      return;
    }

    if (leafletMap) {
      leafletMap.remove();
      leafletMap = null;
    }

    const mapElement = document.getElementById('mapContainer');
    if (!mapElement) return;

    leafletMap = L.map('mapContainer').setView([lat, lon], 16);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors | MuniHuamanga'
    }).addTo(leafletMap);

    const marker = L.marker([lat, lon]).addTo(leafletMap);
    marker.bindPopup(`
      <strong>${nombreComercial || 'Establecimiento'}</strong><br>
      ${direccion || ''}<br>
      <small style="color: #2563eb;">Coordenada: ${tipoCoordenada || 'OFICIAL'}</small>
    `).openPopup();

    // Evento para cotejar ubicación GPS del inspector
    const btnGps = document.getElementById('btnGpsInspector');
    if (btnGps) {
      btnGps.addEventListener('click', () => {
        if (!navigator.geolocation) {
          alert('Su dispositivo no soporta geolocalización GPS.');
          return;
        }

        btnGps.disabled = true;
        btnGps.innerText = 'Obteniendo GPS...';

        navigator.geolocation.getCurrentPosition(
          (pos) => {
            btnGps.disabled = false;
            btnGps.innerText = '📍 Cotejar con mi ubicación actual (GPS)';

            const inspLat = pos.coords.latitude;
            const inspLon = pos.coords.longitude;
            const precision = Math.round(pos.coords.accuracy);

            const distMetros = calcularDistanciaHaversine(inspLat, inspLon, lat, lon);

            if (inspectorMarker) {
              leafletMap.removeLayer(inspectorMarker);
            }

            inspectorMarker = L.circleMarker([inspLat, inspLon], {
              radius: 8,
              fillColor: '#dc2626',
              color: '#ffffff',
              weight: 2,
              opacity: 1,
              fillOpacity: 0.9
            }).addTo(leafletMap);

            inspectorMarker.bindPopup(`<strong>Ubicación Actual del Inspector</strong><br>Precisión: ±${precision}m`).openPopup();

            // Ajustar vista para abarcar ambos puntos
            const group = new L.featureGroup([marker, inspectorMarker]);
            leafletMap.fitBounds(group.getBounds().pad(0.2));

            const gpsResultBox = document.getElementById('gpsResultBox');
            if (gpsResultBox) {
              gpsResultBox.style.display = 'block';
              gpsResultBox.innerHTML = `
                <strong>Distancia aproximada al local:</strong> ${distMetros.toLocaleString()} metros.<br>
                <span style="color: #64748b;">(Precisión satelital del dispositivo: ±${precision}m)</span><br>
                <em style="font-size: 0.8rem; color: #d97706;">* Nota: La distancia calculada es un indicador referencial de proximidad y no constituye por sí sola prueba automática de traslado físico.</em>
              `;
            }
          },
          (err) => {
            btnGps.disabled = false;
            btnGps.innerText = '📍 Cotejar con mi ubicación actual (GPS)';
            alert('No se pudo acceder a la ubicación del dispositivo: ' + err.message);
          },
          { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
      });
    }
  }

  function calcularDistanciaHaversine(lat1, lon1, lat2, lon2) {
    const R = 6371000; // Radio de la Tierra en metros
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(R * c);
  }

  function obtenerBadgeClass(estado, valida) {
    if (!valida) return 'badge-invalida';
    if (!estado) return 'badge-vigente';
    switch (estado.toUpperCase()) {
      case 'EMITIDA':
      case 'VIGENTE':
        return 'badge-vigente';
      case 'TEMPORAL':
        return 'badge-temporal';
      case 'SUSPENDIDA':
        return 'badge-suspendida';
      case 'REVOCADA':
        return 'badge-revocada';
      case 'VENCIDA':
        return 'badge-vencida';
      default:
        return 'badge-invalida';
    }
  }

  function obtenerBadgeTexto(estado, valida) {
    if (!valida) return '✕ SIN VALIDEZ OFICIAL';
    if (!estado) return '✓ LICENCIA OFICIAL VÁLIDA';
    switch (estado.toUpperCase()) {
      case 'EMITIDA':
      case 'VIGENTE':
        return '✓ LICENCIA OFICIAL VIGENTE';
      case 'TEMPORAL':
        return '⏱ LICENCIA TEMPORAL VIGENTE';
      case 'SUSPENDIDA':
        return '⚠ LICENCIA SUSPENDIDA';
      case 'REVOCADA':
        return '✕ LICENCIA REVOCADA';
      case 'VENCIDA':
        return '✕ LICENCIA VENCIDA';
      default:
        return `ESTADO: ${estado}`;
    }
  }

  function formatearFechaHora(fechaIso) {
    if (!fechaIso) return new Date().toLocaleString('es-PE');
    try {
      const d = new Date(fechaIso);
      return d.toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' });
    } catch {
      return fechaIso;
    }
  }

  function renderError(token, errorMsg) {
    resultadoBox.innerHTML = `
      <div class="verify-card">
        <div class="verify-header" style="background: linear-gradient(135deg, #7f1d1d 0%, #b91c1c 100%);">
          <img class="escudo-muni" src="img/escudo-huamanga.png" alt="Escudo de Huamanga" onerror="this.style.display='none'">
          <h1>Municipalidad Provincial de Huamanga</h1>
          <p>Resultado de Verificación de Autenticidad</p>
          <div class="status-badge badge-invalida">
            ✕ LICENCIA NO ENCONTRADA O INVÁLIDA
          </div>
        </div>
        <div class="verify-body" style="text-align: center; padding: 2rem;">
          <p style="color: #dc2626; font-size: 1.05rem; font-weight: 700; margin-bottom: 0.5rem;">
            No se pudo constatar la validez del token: <code>${token}</code>
          </p>
          <p style="color: #64748b; font-size: 0.95rem; margin-bottom: 1.5rem;">
            ${errorMsg}
          </p>
          <div style="background: #f8fafc; padding: 1rem; border-radius: 8px; border: 1px solid #e2e8f0; text-align: left; font-size: 0.85rem; color: #475569;">
            <strong>Recomendaciones para el Administrado / Inspector:</strong>
            <ul style="margin: 0.5rem 0 0 1.25rem; padding: 0;">
              <li>Verifique haber ingresado o escaneado el código QR completo de la licencia oficial.</li>
              <li>Asegúrese de que el trámite haya concluido con Resolución Administrativa favorable debidamente firmada digitalmente.</li>
              <li>Si se encuentra en modo inspector, verifique que su sesión JWT esté activa con permisos de fiscalización.</li>
            </ul>
          </div>
        </div>
      </div>
    `;
  }
});
