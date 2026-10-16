/**
 * portal-ciudadano.js — Fase C.1: Integración Completa del Portal Ciudadano V2
 * 
 * Flujo desacoplado:
 * M01: Autenticación Ciudadana y Cuentas de Administrados
 * M02: Establecimientos Comerciales y Ubicación Geográfica (Leaflet)
 * M03: Solicitud, Borradores preliminares y Validación de Requisitos
 * M04: Carga Documental Segura y Cálculo SHA-256
 * M06: Presentación Formal Atómica e Inmutable (EXP-YYYY-XXXXX)
 * 
 * Ley N° 28976 / TUO D.S. N° 046-2017-PCM / Ley N° 27444 LPAG / Ley N° 29733
 */

// ─── Endpoints REST del Backend ──────────────────────────────────────────────
const API_AUTH            = '/api/auth';
const API_CUENTAS         = '/api/cuentas';
const API_ESTABLECIMIENTOS = '/api/establecimientos';
const API_BORRADORES      = '/api/borradores';
const API_EXPEDIENTES     = '/api/expedientes';
const API_FORMULARIOS     = '/api/formularios';

// ─── Estado global del portal ciudadano ──────────────────────────────────────
let state = {
  usuario: null,            // { username, nombreCompleto, email, rol, ... }
  cuenta: null,             // { id, numeroDocumento, razonSocialONombre, email, ... }
  token: null,              // JWT Bearer token
  pasoActual: 1,
  totalPasos: 4,
  borradorId: null,         // UUID del borrador en backend
  establecimientoId: null,  // UUID del establecimiento seleccionado o creado
  establecimientosPrevios: [],
  mapaLeaflet: null,
  marcadorLeaflet: null,
  expedientePresentado: null,
};

const PROGRESO_PASOS = { 1: 25, 2: 50, 3: 75, 4: 100 };

// =============================================================================
// INICIALIZACIÓN AL CARGAR LA PÁGINA
// =============================================================================
document.addEventListener('DOMContentLoaded', async () => {
  inicializarSesion();
  inicializarDropzone();

  // Si en la URL viene ?tramite=EXP-XXXX, consultar automáticamente
  const params = new URLSearchParams(window.location.search);
  const tramiteParam = params.get('tramite');
  if (tramiteParam) {
    document.getElementById('inputBuscarTramite').value = tramiteParam;
    mostrarPestana('seguimiento');
    consultarTramite(tramiteParam);
  }
});

// =============================================================================
// FASE 1: AUTENTICACIÓN CIUDADANA Y GESTIÓN DE SESIÓN
// =============================================================================
function inicializarSesion() {
  const token = localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
  const usuarioGuardado = localStorage.getItem('jwt_usuario');

  if (token && usuarioGuardado) {
    try {
      state.token = token;
      state.usuario = JSON.parse(usuarioGuardado);
      actualizarUiSesion(true);
      recuperarCuentaDeUsuario(state.usuario.username);
    } catch (e) {
      console.warn('Error al leer sesión previa:', e);
      cerrarSesionCiudadana();
    }
  } else {
    actualizarUiSesion(false);
  }
}

function actualizarUiSesion(autenticado) {
  const anonimoBox = document.getElementById('anonimoControls');
  const autBox = document.getElementById('autenticadoControls');
  const alertaBox = document.getElementById('alertaSesionRequerida');

  if (autenticado && state.usuario) {
    anonimoBox.style.display = 'none';
    autBox.style.display = 'flex';
    if (alertaBox) alertaBox.style.display = 'none';

    document.getElementById('lblCiudadanoNombre').textContent = state.usuario.nombreCompleto || state.usuario.username;
    document.getElementById('lblCiudadanoDoc').textContent = state.usuario.username;

    // Precargar datos en Paso 1
    const txtTitular = document.getElementById('nombreTitular');
    const txtDoc = document.getElementById('documentoIdentidad');
    const txtEmail = document.getElementById('correoElectronico');

    if (txtTitular && !txtTitular.value) txtTitular.value = state.usuario.nombreCompleto || '';
    if (txtDoc && !txtDoc.value) txtDoc.value = state.usuario.username || '';
    if (txtEmail && !txtEmail.value) txtEmail.value = state.usuario.email || '';
  } else {
    anonimoBox.style.display = 'flex';
    autBox.style.display = 'none';
    if (alertaBox) alertaBox.style.display = 'flex';
  }
}

function getAuthHeaders() {
  const headers = { 'Content-Type': 'application/json' };
  if (state.token) {
    headers['Authorization'] = `Bearer ${state.token}`;
  }
  return headers;
}

async function recuperarCuentaDeUsuario(username) {
  try {
    const res = await fetch(`${API_CUENTAS}/documento/${username}`, {
      headers: getAuthHeaders()
    });
    if (res.ok) {
      state.cuenta = await res.json();
      localStorage.setItem('jwt_cuenta_id', state.cuenta.id);
      cargarEstablecimientosDeCuenta();
      cargarMisBorradores();
    } else {
      console.warn('No se pudo recuperar la cuenta por documento:', res.status);
    }
  } catch (err) {
    console.warn('Error de red al recuperar cuenta:', err);
  }
}

// Modales de autenticación
function abrirModalLoginCiudadano() {
  document.getElementById('modalLoginCiudadano').style.display = 'flex';
}

function cerrarModalLoginCiudadano() {
  document.getElementById('modalLoginCiudadano').style.display = 'none';
}

function usarCredencialDemo(doc, pass) {
  document.getElementById('loginDocIdentidad').value = doc;
  document.getElementById('loginPassword').value = pass;
}

async function ejecutarLoginCiudadano() {
  const username = document.getElementById('loginDocIdentidad').value.trim();
  const password = document.getElementById('loginPassword').value.trim();

  if (!username || !password) {
    return alert('⚠️ Ingrese su documento de identidad y contraseña.');
  }

  try {
    const res = await fetch(`${API_AUTH}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Credenciales inválidas. Verifique su número de documento o contraseña.');
    }

    const data = await res.json();
    state.token = data.token;
    state.usuario = {
      id: data.id,
      username: data.username,
      nombreCompleto: data.nombreCompleto,
      email: data.email,
      rol: data.rol
    };

    localStorage.setItem('jwt_token', data.token);
    localStorage.setItem('jwt_usuario', JSON.stringify(state.usuario));

    cerrarModalLoginCiudadano();
    actualizarUiSesion(true);
    await recuperarCuentaDeUsuario(username);

    alert(`👋 ¡Bienvenido/a, ${state.usuario.nombreCompleto || state.usuario.username}! Sesión iniciada con éxito.`);
  } catch (err) {
    alert('❌ Error al iniciar sesión: ' + err.message);
  }
}

function abrirModalRegistroCuenta() {
  cerrarModalLoginCiudadano();
  document.getElementById('modalRegistroCuenta').style.display = 'flex';
}

function cerrarModalRegistroCuenta() {
  document.getElementById('modalRegistroCuenta').style.display = 'none';
}

function alCambiarTipoPersonaReg() {
  const tp = document.getElementById('regTipoPersona').value;
  const selDoc = document.getElementById('regTipoDoc');
  if (tp === 'JURIDICA') {
    selDoc.value = 'RUC';
  } else {
    selDoc.value = 'DNI';
  }
}

async function ejecutarRegistroCuenta() {
  const tipoPersona = document.getElementById('regTipoPersona').value;
  const tipoDocumento = document.getElementById('regTipoDoc').value;
  const numeroDocumento = document.getElementById('regNumDoc').value.trim();
  const razonSocialONombre = document.getElementById('regRazonSocial').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const telefono = document.getElementById('regTelefono').value.trim();
  const direccionFiscal = document.getElementById('regDireccion').value.trim();
  const password = document.getElementById('regPassword').value;
  const passwordConfirm = document.getElementById('regPasswordConfirm').value;

  if (!numeroDocumento || !razonSocialONombre || !email || !password) {
    return alert('⚠️ Por favor complete todos los campos obligatorios (*).');
  }

  if (password.length < 6) {
    return alert('⚠️ La contraseña debe tener al menos 6 caracteres.');
  }

  if (password !== passwordConfirm) {
    return alert('⚠️ Las contraseñas ingresadas no coinciden.');
  }

  const payload = {
    tipoPersona,
    tipoDocumento,
    numeroDocumento,
    razonSocialONombre,
    email,
    telefono,
    direccionFiscal,
    password
  };

  try {
    const res = await fetch(`${API_CUENTAS}/registro`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Error HTTP ${res.status} al crear cuenta.`);
    }

    const cuentaCreada = await res.json();
    cerrarModalRegistroCuenta();
    alert(`🎉 ¡Cuenta creada con éxito para ${cuentaCreada.razonSocialONombre}! Ahora inicie sesión.`);

    // Autocompletar en el login
    document.getElementById('loginDocIdentidad').value = numeroDocumento;
    document.getElementById('loginPassword').value = password;
    abrirModalLoginCiudadano();

  } catch (err) {
    alert('❌ Error al registrar cuenta: ' + err.message);
  }
}

function cerrarSesionCiudadana() {
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('jwt_usuario');
  localStorage.removeItem('jwt_cuenta_id');
  sessionStorage.removeItem('borrador_id_activo');

  state.token = null;
  state.usuario = null;
  state.cuenta = null;
  state.borradorId = null;
  state.establecimientoId = null;

  actualizarUiSesion(false);
  document.getElementById('badgeBorradorEstado').textContent = 'Sin borrador activo';
  document.getElementById('badgeBorradorEstado').style.background = '#e2e8f0';
  document.getElementById('badgeBorradorEstado').style.color = '#334155';

  mostrarPestana('registro');
  irPaso(1);
}

// =============================================================================
// NAVEGACIÓN ENTRE PESTAÑAS PRINCIPALES
// =============================================================================
function mostrarPestana(tab) {
  const secRegistro     = document.getElementById('secRegistro');
  const secMisTramites  = document.getElementById('secMisTramites');
  const secSeguimiento  = document.getElementById('secSeguimiento');

  const btnRegistro     = document.getElementById('tabBtnRegistro');
  const btnMisTramites  = document.getElementById('tabBtnMisTramites');
  const btnSeguimiento  = document.getElementById('tabBtnSeguimiento');

  secRegistro.style.display    = 'none';
  secMisTramites.style.display = 'none';
  secSeguimiento.style.display = 'none';

  btnRegistro.className    = 'btn btn-secondary';
  btnMisTramites.className = 'btn btn-secondary';
  btnSeguimiento.className = 'btn btn-secondary';

  if (tab === 'registro') {
    secRegistro.style.display = 'block';
    btnRegistro.className     = 'btn btn-primary';
    setTimeout(() => { if (state.mapaLeaflet) state.mapaLeaflet.invalidateSize(); }, 200);
  } else if (tab === 'mis-tramites') {
    secMisTramites.style.display = 'block';
    btnMisTramites.className     = 'btn btn-primary';
    cargarMisBorradores();
    cargarMisEstablecimientosTabla();
  } else if (tab === 'seguimiento') {
    secSeguimiento.style.display = 'block';
    btnSeguimiento.className     = 'btn btn-primary';
    consultarTramite();
  }
}

// =============================================================================
// FASE 2: WIZARD DE SOLICITUD V2 Y CONTROL DE PASOS
// =============================================================================
async function irPaso(numeroPaso) {
  const pasoActual = state.pasoActual;

  if (numeroPaso > pasoActual) {
    const valido = await validarYAvanzarPaso(pasoActual);
    if (!valido) return;
  }

  // Ocultar panel actual
  document.getElementById(`wizPanel${pasoActual}`).classList.remove('active');

  // Mostrar nuevo panel
  const panelNuevo = document.getElementById(`wizPanel${numeroPaso}`);
  panelNuevo.classList.add('active');

  // Acciones contextuales por paso
  if (numeroPaso === 2) {
    setTimeout(inicializarMapaLeaflet, 250);
  } else if (numeroPaso === 3) {
    if (state.borradorId) {
      document.getElementById('lblBorradorId').textContent = state.borradorId;
      cargarDocumentosBorrador();
      validarRequisitosBorrador();
    }
  } else if (numeroPaso === 4) {
    construirResumenFinal();
  }

  // Actualizar Stepper y Barra
  actualizarStepper(numeroPaso);
  document.getElementById('wizProgressFill').style.width = PROGRESO_PASOS[numeroPaso] + '%';

  state.pasoActual = numeroPaso;
}

function actualizarStepper(pasoNuevo) {
  for (let i = 1; i <= state.totalPasos; i++) {
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

async function validarYAvanzarPaso(paso) {
  if (paso === 1) {
    if (!state.token || !state.cuenta) {
      alert('⚠️ Para registrar una solicitud formal debe iniciar sesión en el portal.');
      abrirModalLoginCiudadano();
      return false;
    }

    const nombre = valor('nombreTitular');
    const doc    = valor('documentoIdentidad');
    const tel    = valor('telefono');
    const email  = valor('correoElectronico');

    if (!nombre) return mostrarError('El nombre del titular o razón social es obligatorio.');
    if (!doc)    return mostrarError('El documento de identidad es obligatorio.');
    if (!tel)    return mostrarError('El teléfono celular de contacto es obligatorio.');
    if (!email)  return mostrarError('El correo electrónico de notificación es obligatorio.');

    return true;
  }

  if (paso === 2) {
    const nombreCom = valor('nombreComercial');
    const ciiu = valor('ciiuCodigo');
    const giro = valor('giroNegocio');
    const dir = valor('direccionEstablecimiento');
    const area = parseFloat(document.getElementById('areaMetrosCuadrados').value);

    if (!nombreCom) return mostrarError('El nombre comercial del establecimiento es obligatorio.');
    if (!ciiu)      return mostrarError('El código CIIU es obligatorio.');
    if (!giro)      return mostrarError('El giro o actividad económica es obligatorio.');
    if (!dir)       return mostrarError('La dirección física del establecimiento es obligatoria.');
    if (!area || area <= 0) return mostrarError('El área en m² debe ser mayor a 0.');

    // Asegurar o crear establecimiento y luego crear/actualizar borrador V2
    const btnSig2 = document.getElementById('btnSiguiente2');
    btnSig2.disabled = true;
    btnSig2.innerHTML = '<span class="spinner"></span> Asegurando borrador...';

    try {
      await asegurarEstablecimientoYBorrador();
      btnSig2.disabled = false;
      btnSig2.innerHTML = 'Siguiente: Documentación y Requisitos →';
      return true;
    } catch (err) {
      btnSig2.disabled = false;
      btnSig2.innerHTML = 'Siguiente: Documentación y Requisitos →';
      alert('❌ Error al registrar borrador: ' + err.message);
      return false;
    }
  }

  if (paso === 3) {
    // Al pasar de paso 3 al 4, guardar avance de pago si hay datos
    await guardarAvanceBorrador(false);
    return true;
  }

  return true;
}

// =============================================================================
// FASE 3: GEOLOCALIZACIÓN Y GESTIÓN DE ESTABLECIMIENTOS (LEAFLET)
// =============================================================================
function inicializarMapaLeaflet() {
  const container = document.getElementById('mapaEstablecimiento');
  if (!container) return;

  const latDefault = parseFloat(document.getElementById('geoLatitud').value) || -13.16315;
  const lngDefault = parseFloat(document.getElementById('geoLongitud').value) || -74.22389;

  if (!state.mapaLeaflet) {
    state.mapaLeaflet = L.map('mapaEstablecimiento').setView([latDefault, lngDefault], 16);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors | MPH'
    }).addTo(state.mapaLeaflet);

    state.marcadorLeaflet = L.marker([latDefault, lngDefault], { draggable: true }).addTo(state.mapaLeaflet);

    state.marcadorLeaflet.on('dragend', (e) => {
      const pos = e.target.getLatLng();
      actualizarCoordenadasEnPantalla(pos.lat, pos.lng);
    });

    state.mapaLeaflet.on('click', (e) => {
      state.marcadorLeaflet.setLatLng(e.latlng);
      actualizarCoordenadasEnPantalla(e.latlng.lat, e.latlng.lng);
    });
  } else {
    state.mapaLeaflet.invalidateSize();
  }
}

function actualizarCoordenadasEnPantalla(lat, lng) {
  const fLat = lat.toFixed(5);
  const fLng = lng.toFixed(5);

  document.getElementById('geoLatitud').value = fLat;
  document.getElementById('geoLongitud').value = fLng;
  document.getElementById('lblLatitud').textContent = fLat;
  document.getElementById('lblLongitud').textContent = fLng;
}

function solicitarGeolocalizacionDispositivo() {
  const consent = confirm('🧭 CONSENTIMIENTO DE GEOLOCALIZACIÓN:\n\n' +
    'Se solicitará acceso al sensor GPS de su dispositivo para fijar la ubicación declarada del local comercial.\n\n' +
    '¿Autoriza capturar sus coordenadas actuales?');

  if (!consent) return;

  if (!navigator.geolocation) {
    return alert('⚠️ Su navegador no soporta geolocalización HTML5.');
  }

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      actualizarCoordenadasEnPantalla(lat, lng);

      if (state.mapaLeaflet && state.marcadorLeaflet) {
        state.mapaLeaflet.setView([lat, lng], 17);
        state.marcadorLeaflet.setLatLng([lat, lng]);
      }
      alert('📍 Coordenadas capturadas con éxito desde su dispositivo.');
    },
    (err) => {
      alert('⚠️ No se pudo obtener la ubicación GPS: ' + err.message);
    },
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

function alCambiarArea() {
  const area = parseFloat(document.getElementById('areaMetrosCuadrados').value) || 0;
  const hint = document.getElementById('hintNivelRiesgoEstimado');
  if (area >= 500) {
    hint.textContent = '🔴 Riesgo Alto / Muy Alto estimado (Requiere ITSE Previa antes de abrir)';
    hint.style.color = '#dc2626';
    document.getElementById('selModalidadTramite').value = 'SA_ITSE_PREVIA';
  } else {
    hint.textContent = '🟡 Riesgo Bajo / Medio estimado (Inspección ITSE Posterior)';
    hint.style.color = '#d97706';
    document.getElementById('selModalidadTramite').value = 'SA_ITSE_POSTERIOR';
  }
}

async function cargarEstablecimientosDeCuenta() {
  if (!state.cuenta) return;
  try {
    const res = await fetch(`${API_ESTABLECIMIENTOS}/cuenta/${state.cuenta.id}`, {
      headers: getAuthHeaders()
    });
    if (res.ok) {
      state.establecimientosPrevios = await res.json();
      poblarSelectEstablecimientos();
    }
  } catch (e) {
    console.warn('Error al listar establecimientos:', e);
  }
}

function poblarSelectEstablecimientos() {
  const sel = document.getElementById('selEstablecimientoExistente');
  sel.innerHTML = '<option value="NUEVO">➕ Registrar un nuevo establecimiento comercial</option>';

  state.establecimientosPrevios.forEach(est => {
    const opt = document.createElement('option');
    opt.value = est.id;
    opt.textContent = `📍 ${est.nombreComercial} — ${est.direccion} (${est.areaMetrosCuadrados} m²)`;
    sel.appendChild(opt);
  });
}

function alCambiarEstablecimientoExistente() {
  const val = document.getElementById('selEstablecimientoExistente').value;
  if (val === 'NUEVO') {
    state.establecimientoId = null;
    return;
  }

  const est = state.establecimientosPrevios.find(e => e.id === val);
  if (!est) return;

  state.establecimientoId = est.id;
  document.getElementById('nombreComercial').value = est.nombreComercial || '';
  document.getElementById('ciiuCodigo').value = est.ciiuCodigo || 'G4772';
  document.getElementById('giroNegocio').value = est.giroPrincipal || '';
  document.getElementById('direccionEstablecimiento').value = est.direccion || '';
  document.getElementById('referenciaEstablecimiento').value = est.referencia || '';
  document.getElementById('areaMetrosCuadrados').value = est.areaMetrosCuadrados || 85.5;

  if (est.latitud && est.longitud) {
    actualizarCoordenadasEnPantalla(parseFloat(est.latitud), parseFloat(est.longitud));
    if (state.mapaLeaflet && state.marcadorLeaflet) {
      state.mapaLeaflet.setView([est.latitud, est.longitud], 16);
      state.marcadorLeaflet.setLatLng([est.latitud, est.longitud]);
    }
  }
  alCambiarArea();
}

// Creación o asociación de Establecimiento y Borrador V2
async function asegurarEstablecimientoYBorrador() {
  const areaM2 = parseFloat(document.getElementById('areaMetrosCuadrados').value);
  const nombreCom = valor('nombreComercial');
  const ciiu = valor('ciiuCodigo');
  const giro = valor('giroNegocio');
  const direccion = valor('direccionEstablecimiento');
  const referencia = valor('referenciaEstablecimiento');
  const lat = parseFloat(document.getElementById('geoLatitud').value) || -13.16315;
  const lng = parseFloat(document.getElementById('geoLongitud').value) || -74.22389;
  const numPisos = parseInt(document.getElementById('selNumPisos').value) || 1;
  const modalidad = document.getElementById('selModalidadTramite').value;

  // 1. Si no hay establecimiento seleccionado, registrar nuevo
  if (!state.establecimientoId) {
    const estDto = {
      nombreComercial: nombreCom,
      direccion: direccion,
      distrito: 'Ayacucho',
      provincia: 'Huamanga',
      departamento: 'Ayacucho',
      referencia: referencia || 'Centro de Huamanga',
      latitud: lat,
      longitud: lng,
      ciiuCodigo: ciiu,
      giroPrincipal: giro,
      areaMetrosCuadrados: areaM2
    };

    const resEst = await fetch(`${API_ESTABLECIMIENTOS}/cuenta/${state.cuenta.id}`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(estDto)
    });

    if (!resEst.ok) {
      const err = await resEst.json().catch(() => ({}));
      throw new Error(err.message || 'Error al registrar establecimiento comercial');
    }

    const estCreado = await resEst.json();
    state.establecimientoId = estCreado.id;
    await cargarEstablecimientosDeCuenta();
  }

  // 2. Crear o actualizar Borrador V2
  if (!state.borradorId) {
    const crearBorradorDto = {
      cuentaId: state.cuenta.id,
      establecimientoId: state.establecimientoId,
      modalidadTramite: modalidad,
      nombreComercialDeclarado: nombreCom,
      giroNegocioDeclarado: giro,
      actividadDetallada: giro,
      areaMetrosCuadrados: areaM2,
      aforoPersonas: Math.max(10, Math.round(areaM2 / 2)),
      numeroPisos: numPisos,
      horarioInicio: '08:00',
      horarioFin: '20:00',
      telefonoContacto: valor('telefono'),
      correoNotificacion: valor('correoElectronico'),
      autorizaNotificacion: true,
      nivelRiesgoDeclarado: areaM2 >= 500 ? 'ALTO' : 'BAJO'
    };

    const resBorr = await fetch(API_BORRADORES, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(crearBorradorDto)
    });

    if (!resBorr.ok) {
      const err = await resBorr.json().catch(() => ({}));
      throw new Error(err.message || 'Error al crear borrador de solicitud');
    }

    const borrador = await resBorr.json();
    state.borradorId = borrador.id;
    sessionStorage.setItem('borrador_id_activo', borrador.id);

    document.getElementById('badgeBorradorEstado').textContent = `Borrador Activo: ${borrador.id.substring(0, 8)}...`;
    document.getElementById('badgeBorradorEstado').style.background = '#dbeafe';
    document.getElementById('badgeBorradorEstado').style.color = '#1e40af';
  } else {
    // Actualizar datos del borrador
    await guardarAvanceBorrador(false);
  }
}

async function guardarAvanceBorrador(mostrarNotificacion = true) {
  if (!state.borradorId) return;

  const areaM2 = parseFloat(document.getElementById('areaMetrosCuadrados').value) || 85.5;
  const numPisos = parseInt(document.getElementById('selNumPisos')?.value) || 1;

  const payload = {
    modalidadTramite: document.getElementById('selModalidadTramite').value,
    nombreComercialDeclarado: valor('nombreComercial'),
    giroNegocioDeclarado: valor('giroNegocio'),
    actividadDetallada: valor('giroNegocio'),
    areaMetrosCuadrados: areaM2,
    aforoPersonas: Math.max(10, Math.round(areaM2 / 2)),
    numeroPisos: numPisos,
    horarioInicio: '08:00',
    horarioFin: '20:00',
    telefonoContacto: valor('telefono'),
    correoNotificacion: valor('correoElectronico'),
    autorizaNotificacion: true,
    nivelRiesgoDeclarado: areaM2 >= 500 ? 'ALTO' : 'BAJO'
  };

  try {
    const res = await fetch(`${API_BORRADORES}/${state.borradorId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(payload)
    });

    if (res.ok && mostrarNotificacion) {
      alert('💾 Avance de solicitud guardado exitosamente.');
    }
  } catch (e) {
    console.warn('Error al guardar avance:', e);
  }
}

// =============================================================================
// FASE 4: CARGA DOCUMENTAL SEGURA Y HASH SHA-256
// =============================================================================
function inicializarDropzone() {
  const dropzone = document.getElementById('dropzoneBox');
  if (!dropzone) return;

  ['dragenter', 'dragover'].forEach(eventName => {
    dropzone.addEventListener(eventName, (e) => {
      e.preventDefault();
      dropzone.classList.add('dragover');
    }, false);
  });

  ['dragleave', 'drop'].forEach(eventName => {
    dropzone.addEventListener(eventName, (e) => {
      e.preventDefault();
      dropzone.classList.remove('dragover');
    }, false);
  });

  dropzone.addEventListener('drop', (e) => {
    const dt = e.dataTransfer;
    const files = dt.files;
    if (files.length > 0) {
      procesarArchivo(files[0]);
    }
  });
}

function procesarArchivoSeleccionado(input) {
  if (input.files && input.files.length > 0) {
    procesarArchivo(input.files[0]);
  }
}

async function procesarArchivo(file) {
  if (!state.borradorId) {
    return alert('⚠️ No hay borrador activo para adjuntar documentos. Vuelva al paso anterior.');
  }

  // Validaciones cliente
  const extensionesPermitidas = ['.pdf', '.png', '.jpg', '.jpeg'];
  const ext = '.' + file.name.split('.').pop().toLowerCase();
  if (!extensionesPermitidas.includes(ext)) {
    return alert('❌ Tipo de archivo no permitido. Solo se admite PDF, PNG y JPG.');
  }

  const maxBytes = 10 * 1024 * 1024; // 10 MB
  if (file.size > maxBytes) {
    return alert('❌ El archivo supera el tamaño máximo permitido de 10 MB.');
  }

  const tipoDocumento = document.getElementById('selTipoDocumentoUpload').value;
  const progresoBox = document.getElementById('progresoUploadBox');
  const barProgreso = document.getElementById('barProgresoUpload');
  const txtPorcentaje = document.getElementById('txtPorcentajeUpload');

  progresoBox.style.display = 'block';
  barProgreso.style.width = '25%';
  txtPorcentaje.textContent = '25% — Calculando SHA-256...';

  try {
    const arrayBuffer = await file.arrayBuffer();

    // 1. Cálculo criptográfico SHA-256 preliminar en navegador
    const hashBuffer = await crypto.subtle.digest('SHA-256', arrayBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const sha256Hex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

    barProgreso.style.width = '60%';
    txtPorcentaje.textContent = '60% — Codificando Base64...';

    // 2. Convertir a Base64
    let binary = '';
    const bytes = new Uint8Array(arrayBuffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    const base64 = window.btoa(binary);

    barProgreso.style.width = '85%';
    txtPorcentaje.textContent = '85% — Enviando al servidor seguro...';

    const payload = {
      tipoDocumento: tipoDocumento,
      nombreArchivo: file.name,
      tipoContenido: file.type || 'application/pdf',
      contenidoBase64: base64,
      descripcion: `Hash cliente SHA-256: ${sha256Hex}`
    };

    const res = await fetch(`${API_BORRADORES}/${state.borradorId}/documentos`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Error HTTP ${res.status}`);
    }

    barProgreso.style.width = '100%';
    txtPorcentaje.textContent = '100% — Documento recibido y asegurado';

    setTimeout(() => { progresoBox.style.display = 'none'; }, 1000);

    alert(`✅ Documento "${file.name}" cargado exitosamente.\nHash SHA-256 verificado: ${sha256Hex.substring(0, 16)}...`);

    // Refrescar lista de documentos y checklist
    cargarDocumentosBorrador();
    validarRequisitosBorrador();

  } catch (err) {
    progresoBox.style.display = 'none';
    alert('❌ Error al procesar documento: ' + err.message);
  }
}

async function cargarDocumentosBorrador() {
  if (!state.borradorId) return;
  const container = document.getElementById('listaDocumentosBorrador');

  try {
    const res = await fetch(`${API_BORRADORES}/${state.borradorId}/documentos`, {
      headers: getAuthHeaders()
    });

    if (!res.ok) throw new Error('Error al consultar documentos');
    const docs = await res.json();

    if (!docs || docs.length === 0) {
      container.innerHTML = '<p style="color: var(--text-muted); font-size: 0.85rem; font-style: italic;">No hay documentos adjuntos aún.</p>';
      return;
    }

    container.innerHTML = docs.map(d => `
      <div class="doc-item-row">
        <div>
          <strong style="font-size: 0.85rem; color: #1e3a8a;">[${d.tipoDocumento}]</strong>
          <span style="font-size: 0.85rem; font-weight: 600; margin-left: 0.25rem;">${d.nombreArchivo}</span>
          <span style="font-size: 0.75rem; color: var(--text-muted); margin-left: 0.5rem;">(${Math.round(d.tamanioBytes / 1024)} KB)</span>
          <div style="margin-top: 0.25rem;">
            <span class="doc-hash-pill" title="Hash SHA-256 inmutable">SHA-256: ${d.hashSha256 ? d.hashSha256.substring(0, 24) + '...' : '-'}</span>
            <span class="badge" style="font-size: 0.7rem; background: #fef3c7; color: #92400e; margin-left: 0.35rem;">🛡️ Antivirus: Escaneo en curso</span>
          </div>
        </div>
        <div>
          <button type="button" class="btn btn-secondary btn-sm" onclick="eliminarDocumentoBorrador('${d.id}')" style="color: #dc2626; border-color: #fca5a5; font-size: 0.78rem;">
            🗑️ Eliminar
          </button>
        </div>
      </div>
    `).join('');

  } catch (e) {
    container.innerHTML = '<p style="color: #dc2626; font-size: 0.85rem;">Error al cargar lista de documentos.</p>';
  }
}

async function eliminarDocumentoBorrador(docId) {
  if (!confirm('¿Está seguro de eliminar este documento del borrador?')) return;
  try {
    const res = await fetch(`${API_BORRADORES}/${state.borradorId}/documentos/${docId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    });
    if (res.ok) {
      cargarDocumentosBorrador();
      validarRequisitosBorrador();
    }
  } catch (e) {
    alert('Error al eliminar documento: ' + e.message);
  }
}

async function validarRequisitosBorrador() {
  if (!state.borradorId) return;
  const lista = document.getElementById('listaRequisitosPendientes');

  try {
    const res = await fetch(`${API_BORRADORES}/${state.borradorId}/requisitos-pendientes`, {
      headers: getAuthHeaders()
    });

    if (!res.ok) return;
    const val = await res.json();

    const apto = val.aptoParaPresentacion !== undefined ? val.aptoParaPresentacion : val.aptoParaPresentar;
    if (apto) {
      lista.innerHTML = `
        <li style="color: #15803d; font-weight: 700;">✅ ¡Todos los requisitos obligatorios han sido cumplidos! Su trámite está APTO para presentación formal.</li>
      `;
    } else {
      const faltantes = [...(val.camposFaltantes || []), ...(val.documentosFaltantes || [])];
      lista.innerHTML = faltantes.map(r => `
        <li style="color: #b91c1c;">⚠️ Pendiente: <strong>${r}</strong></li>
      `).join('') || '<li style="color: #b45309;">Requisitos en validación...</li>';
    }
  } catch (e) {
    console.warn('Error al validar requisitos:', e);
  }
}

// =============================================================================
// FASE 5: INTEGRACIÓN NATIVA CON FORMATO ANEXO 1
// =============================================================================
function abrirAnexo1ConBorrador() {
  if (!state.borradorId) {
    return alert('⚠️ Primero complete los datos del establecimiento para generar el borrador.');
  }
  const url = `formato-anexo1.html?borradorId=${state.borradorId}&cuentaId=${state.cuenta ? state.cuenta.id : ''}`;
  window.open(url, '_blank');
}

async function descargarAnexoBorrador(tipo) {
  if (!state.borradorId) {
    return alert('⚠️ Guarde su borrador antes de descargar los formatos oficiales.');
  }
  const url = `${API_BORRADORES}/${state.borradorId}/formularios/${tipo}/pdf`;
  window.open(url, '_blank');
}

// =============================================================================
// FASE 6: RESUMEN Y PRESENTACIÓN FORMAL IDEMPOTENTE (M06)
// =============================================================================
function construirResumenFinal() {
  const area = parseFloat(document.getElementById('areaMetrosCuadrados').value) || 85.5;
  const tasa = area >= 500 ? 'S/. 345.20 (ITSE Previa)' : 'S/. 154.50 (ITSE Posterior)';
  document.getElementById('txtMontoTupaEstimado').textContent = tasa;

  const items = [
    { label: 'Titular / Administrado',    valor: valor('nombreTitular') },
    { label: 'Documento de Identidad',   valor: valor('documentoIdentidad') },
    { label: 'Modalidad de Licencia',    valor: document.getElementById('selModalidadTramite').selectedOptions[0].text },
    { label: 'Teléfono / Correo',        valor: `${valor('telefono')} | ${valor('correoElectronico')}` },
    { label: 'Establecimiento Comercial', valor: valor('nombreComercial') },
    { label: 'Giro y Código CIIU',       valor: `${valor('giroNegocio')} (${valor('ciiuCodigo')})` },
    { label: 'Dirección y Referencia',   valor: `${valor('direccionEstablecimiento')} (${valor('referenciaEstablecimiento') || '-'})` },
    { label: 'Coordenadas Oficiales',    valor: `Lat: ${valor('geoLatitud')} | Lng: ${valor('geoLongitud')}` },
    { label: 'Área Total Declarada',     valor: `${area} m² (${valor('selNumPisos')} pisos)` },
    { label: 'Borrador Identificador',   valor: state.borradorId || 'Borrador local' }
  ];

  document.getElementById('resumenDatos').innerHTML = items.map(it => `
    <div class="resumen-item">
      <strong>${it.label}</strong>
      <span>${it.valor || '-'}</span>
    </div>
  `).join('');
}

async function presentarSolicitudFormal() {
  if (!state.borradorId) {
    return alert('⚠️ No hay borrador de solicitud activo para presentar.');
  }

  const chk = document.getElementById('chkVeracidad');
  if (!chk.checked) {
    return alert('⚠️ Debe aceptar expresamente la Declaración Jurada de Veracidad (Art. 42° Ley N° 27444).');
  }

  const btn = document.getElementById('btnEnviar');
  // Control de idempotencia (prevenir dobles clics)
  if (btn.disabled) return;
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Formalizando presentación en Mesa de Partes...';

  const presentarPayload = {
    declaracionJuradaAceptada: true,
    confirmacionPresentacion: true,
    comentarios: 'Presentación formal vía Portal Ciudadano V2 — MuniHuamanga'
  };

  try {
    const res = await fetch(`${API_BORRADORES}/${state.borradorId}/presentar`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(presentarPayload)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Error HTTP ${res.status} al formalizar presentación`);
    }

    const exp = await res.json();
    state.expedientePresentado = exp;

    // Éxito: limpiar borrador activo de la sesión
    sessionStorage.removeItem('borrador_id_activo');
    state.borradorId = null;

    mostrarBannerExitoPresentacion(exp);

  } catch (err) {
    alert('❌ No se pudo completar la presentación formal:\n' + err.message);
    btn.disabled = false;
    btn.innerHTML = '🚀 Formalizar y Presentar Solicitud a Mesa de Partes';
  }
}

function mostrarBannerExitoPresentacion(exp) {
  document.getElementById('zonaBtnEnviar').style.display = 'none';
  document.getElementById('wizNavPaso4').style.display = 'none';
  document.getElementById('resumenDatos').style.display = 'none';
  document.getElementById('chkVeracidad').parentElement.parentElement.style.display = 'none';

  document.getElementById('bannerExito').style.display = 'block';
  document.getElementById('txtNumeroTramiteGenerado').textContent = exp.numeroTramite;

  document.getElementById('inputBuscarTramite').value = exp.numeroTramite;
  document.getElementById('wizProgressFill').style.width = '100%';
}

function irASeguimientoDesdeExito() {
  mostrarPestana('seguimiento');
  consultarTramite();
}

function iniciarNuevoTramite() {
  state.borradorId = null;
  state.expedientePresentado = null;
  sessionStorage.removeItem('borrador_id_activo');

  document.getElementById('zonaBtnEnviar').style.display = 'block';
  document.getElementById('wizNavPaso4').style.display = 'flex';
  document.getElementById('resumenDatos').style.display = 'grid';
  document.getElementById('chkVeracidad').parentElement.parentElement.style.display = 'block';
  document.getElementById('chkVeracidad').checked = false;
  document.getElementById('bannerExito').style.display = 'none';

  const btn = document.getElementById('btnEnviar');
  btn.disabled = false;
  btn.innerHTML = '🚀 Formalizar y Presentar Solicitud a Mesa de Partes';

  document.getElementById('badgeBorradorEstado').textContent = 'Sin borrador activo';
  document.getElementById('badgeBorradorEstado').style.background = '#e2e8f0';
  document.getElementById('badgeBorradorEstado').style.color = '#334155';

  irPaso(1);
}

// =============================================================================
// SECCIÓN MIS TRÁMITES Y BORRADORES
// =============================================================================
async function cargarMisBorradores() {
  if (!state.cuenta) return;
  const tbody = document.getElementById('tablaBorradoresBody');
  if (!tbody) return;

  try {
    const res = await fetch(`${API_BORRADORES}/cuenta/${state.cuenta.id}`, {
      headers: getAuthHeaders()
    });
    if (!res.ok) throw new Error('Error al listar borradores');
    const borradores = await res.json();

    if (!borradores || borradores.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">No tiene borradores pendientes. Puede iniciar una nueva solicitud.</td></tr>';
      return;
    }

    tbody.innerHTML = borradores.map(b => `
      <tr>
        <td><code>${b.id.substring(0, 8)}...</code></td>
        <td><strong>${b.nombreComercialDeclarado || 'Sin nombre comercial'}</strong><br><small style="color: var(--text-muted);">${b.giroNegocioDeclarado || '-'}</small></td>
        <td>${b.fechaActualizacion ? new Date(b.fechaActualizacion).toLocaleString() : '-'}</td>
        <td><span class="badge" style="background: #e0f2fe; color: #0369a1;">${b.estado}</span></td>
        <td>
          <button type="button" class="btn btn-primary btn-sm" onclick="retomarBorrador('${b.id}')" style="font-size: 0.8rem; padding: 0.3rem 0.6rem;">
            ✏️ Continuar
          </button>
        </td>
      </tr>
    `).join('');

  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: #dc2626;">Error al cargar borradores.</td></tr>';
  }
}

async function retomarBorrador(borradorId) {
  try {
    const res = await fetch(`${API_BORRADORES}/${borradorId}`, {
      headers: getAuthHeaders()
    });
    if (!res.ok) throw new Error('No se pudo recuperar el borrador');
    const b = await res.json();

    state.borradorId = b.id;
    sessionStorage.setItem('borrador_id_activo', b.id);

    if (b.establecimiento) {
      state.establecimientoId = b.establecimiento.id;
    }

    // Cargar datos en el formulario
    document.getElementById('nombreComercial').value = b.nombreComercialDeclarado || '';
    document.getElementById('giroNegocio').value = b.giroNegocioDeclarado || '';
    document.getElementById('areaMetrosCuadrados').value = b.areaMetrosCuadrados || 85.5;
    document.getElementById('telefono').value = b.telefonoContacto || '';
    document.getElementById('correoElectronico').value = b.correoNotificacion || '';
    if (b.modalidadTramite) document.getElementById('selModalidadTramite').value = b.modalidadTramite;

    document.getElementById('badgeBorradorEstado').textContent = `Borrador Activo: ${b.id.substring(0, 8)}...`;
    document.getElementById('badgeBorradorEstado').style.background = '#dbeafe';
    document.getElementById('badgeBorradorEstado').style.color = '#1e40af';

    mostrarPestana('registro');
    irPaso(2);
    alert(`📂 Borrador recuperado con éxito: ${b.nombreComercialDeclarado || b.id}`);
  } catch (err) {
    alert('❌ Error al retomar borrador: ' + err.message);
  }
}

async function cargarMisEstablecimientosTabla() {
  if (!state.cuenta) return;
  const tbody = document.getElementById('tablaMisEstablecimientosBody');
  if (!tbody) return;

  try {
    const res = await fetch(`${API_ESTABLECIMIENTOS}/cuenta/${state.cuenta.id}`, {
      headers: getAuthHeaders()
    });
    if (!res.ok) throw new Error('Error al listar establecimientos');
    const locales = await res.json();

    if (!locales || locales.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">No tiene establecimientos registrados aún.</td></tr>';
      return;
    }

    tbody.innerHTML = locales.map(l => `
      <tr>
        <td><strong>${l.nombreComercial}</strong></td>
        <td>${l.direccion}</td>
        <td>${l.areaMetrosCuadrados} m²</td>
        <td>${l.giroPrincipal} (${l.ciiuCodigo})</td>
        <td><code>${l.latitud}, ${l.longitud}</code></td>
      </tr>
    `).join('');

  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: #dc2626;">Error al cargar establecimientos.</td></tr>';
  }
}

// =============================================================================
// SEGUIMIENTO EN LÍNEA DE EXPEDIENTE (M10, M11, M12)
// =============================================================================
async function consultarTramite(tramiteManual) {
  const tramite = tramiteManual || valor('inputBuscarTramite') || 'EXP-2026-00101';
  if (!tramite) return alert('Por favor ingrese un número de expediente.');

  try {
    const res = await fetch(`${API_EXPEDIENTES}/tramite/${tramite}`);
    if (!res.ok) {
      throw new Error(`Expediente "${tramite}" no encontrado en el sistema.`);
    }

    const exp = await res.json();
    mostrarResultadoSeguimiento(exp);
  } catch (err) {
    alert('⚠️ ' + err.message);
  }
}

function mostrarResultadoSeguimiento(exp) {
  document.getElementById('resultadoConsulta').style.display = 'block';

  document.getElementById('resNumeroTramite').textContent = exp.numeroTramite || '-';
  document.getElementById('resTitular').textContent = exp.nombreTitular || '-';
  document.getElementById('resGiro').textContent = exp.giroNegocio || '-';
  document.getElementById('resDireccion').textContent = exp.direccionEstablecimiento || '-';
  document.getElementById('resNivelRiesgo').textContent = exp.nivelRiesgo || '-';
  document.getElementById('resMontoTasa').textContent = exp.montoTasa ? `S/. ${parseFloat(exp.montoTasa).toFixed(2)}` : 'S/. 154.50';
  document.getElementById('resPlazoRestante').textContent = exp.diasRestantes != null ? `${exp.diasRestantes} días hábiles restantes` : '15 días hábiles';

  // Badge de Estado
  const badgeEl = document.getElementById('resEstadoBadge');
  badgeEl.className = 'badge';
  badgeEl.textContent = exp.estado || 'PRESENTADO';

  if (exp.estado === 'APROBADO' || exp.estado === 'EMITIDO') {
    badgeEl.className = 'badge badge-aprobado';
  } else if (exp.estado === 'OBSERVADO') {
    badgeEl.className = 'badge badge-observado';
  } else if (exp.estado === 'APTO_PARA_RESOLUCION') {
    badgeEl.className = 'badge badge-resolucion';
  } else {
    badgeEl.className = 'badge badge-formatos';
  }

  // Iluminar línea de tiempo V2
  ['step-PRESENTADO', 'step-EN_EVALUACION', 'step-APTO_PARA_RESOLUCION', 'step-APROBADO'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active', 'completed');
  });
  const st1 = document.getElementById('step-PRESENTADO');
  const st2 = document.getElementById('step-EN_EVALUACION');
  const st3 = document.getElementById('step-APTO_PARA_RESOLUCION');
  const st4 = document.getElementById('step-APROBADO');

  if (st1) st1.classList.add('completed');
  if (exp.estado === 'PRESENTADO') {
    if (st1) st1.classList.add('active');
  } else if (exp.estado === 'EN_EVALUACION' || exp.estado === 'OBSERVADO' || exp.estado === 'SUBSANADO' || exp.estado === 'EN_EVALUACION_FINAL') {
    if (st1) st1.classList.add('completed');
    if (st2) st2.classList.add('active');
  } else if (exp.estado === 'APTO_PARA_RESOLUCION') {
    if (st1) st1.classList.add('completed');
    if (st2) st2.classList.add('completed');
    if (st3) st3.classList.add('active');
  } else if (exp.estado === 'APROBADO' || exp.estado === 'EMITIDO') {
    if (st1) st1.classList.add('completed');
    if (st2) st2.classList.add('completed');
    if (st3) st3.classList.add('completed');
    if (st4) st4.classList.add('active');
  }

  // Ficha Aprobado / QR
  const boxLic = document.getElementById('boxLicenciaAprobada');
  if (exp.estado === 'APROBADO' && exp.codigoLicencia) {
    boxLic.style.display = 'flex';
    document.getElementById('txtCodigoLicencia').textContent = exp.codigoLicencia;
    if (exp.qrBase64) {
      document.getElementById('imgQrLicencia').src = 'data:image/png;base64,' + exp.qrBase64;
    }
  } else {
    boxLic.style.display = 'none';
  }

  // Ficha Observado
  const boxObs = document.getElementById('boxExpedienteObservado');
  if (exp.estado === 'OBSERVADO') {
    boxObs.style.display = 'flex';
    document.getElementById('txtMotivoObservacionCiudadano').textContent = exp.motivoObservacion || 'Requisitos técnicos pendientes de subsanación.';
  } else {
    boxObs.style.display = 'none';
  }

  // Documentos descargables
  const btnDj = document.getElementById('btnDescargarDj');
  btnDj.href = `/api/formularios/anexo-1/${exp.id}/pdf`;

  const btnLic = document.getElementById('btnDescargarLicencia');
  if (exp.estado === 'APROBADO') {
    btnLic.style.display = 'inline-flex';
    btnLic.href = `/api/licencias/expediente/${exp.id}/pdf`;
  } else {
    btnLic.style.display = 'none';
  }

  // Historial de auditoría
  const tbodyHist = document.getElementById('tablaHistorialBody');
  if (exp.historial && exp.historial.length > 0) {
    tbodyHist.innerHTML = exp.historial.map(h => `
      <tr>
        <td>${h.fechaHora ? new Date(h.fechaHora).toLocaleString() : '-'}</td>
        <td>${h.estadoAnterior || '-'}</td>
        <td><strong>${h.estadoNuevo}</strong></td>
        <td>${h.usuario || 'SISTEMA'}</td>
        <td>${h.motivo || '-'}</td>
      </tr>
    `).join('');
  } else {
    tbodyHist.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">Sin transiciones registradas.</td></tr>';
  }
}

// =============================================================================
// FUNCIONES AUXILIARES Y DEMO
// =============================================================================
function valor(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : '';
}

function mostrarError(msg) {
  alert('⚠️ ' + msg);
  return false;
}

function llenarDatosDemo() {
  document.getElementById('nombreTitular').value = 'María Quispe Huamán';
  document.getElementById('documentoIdentidad').value = '42567891';
  document.getElementById('razonSocial').value = 'FARMACIA SAN JUAN BAUTISTA S.A.C.';
  document.getElementById('telefono').value = '966123456';
  document.getElementById('correoElectronico').value = 'maria.quispe@gmail.com';

  document.getElementById('nombreComercial').value = 'Farmacia San Juan Bautista V2';
  document.getElementById('ciiuCodigo').value = 'G4772';
  document.getElementById('giroNegocio').value = 'Venta minorista de productos farmacéuticos y artículos de tocador';
  document.getElementById('direccionEstablecimiento').value = 'Jr. 28 de Julio N° 450, Centro Histórico';
  document.getElementById('referenciaEstablecimiento').value = 'A media cuadra del Templo de San Juan de Dios';
  document.getElementById('areaMetrosCuadrados').value = '85.50';
  document.getElementById('selNumPisos').value = '1';

  actualizarCoordenadasEnPantalla(-13.16315, -74.22389);
  if (state.mapaLeaflet && state.marcadorLeaflet) {
    state.mapaLeaflet.setView([-13.16315, -74.22389], 16);
    state.marcadorLeaflet.setLatLng([-13.16315, -74.22389]);
  }

  alCambiarArea();
  alert('🪄 Datos de prueba cargados con éxito.');
}

function abrirMesaAyuda(tipo) {
  const modal = document.getElementById('modalMesaAyuda');
  const tit = document.getElementById('mesaAyudaTitulo');
  const body = document.getElementById('mesaAyudaContenido');

  if (tipo === 'anexo3') {
    tit.textContent = 'Mesa de Ayuda — Matriz de Riesgo ITSE (Anexo 3)';
    body.innerHTML = `
      <p>La Matriz de Riesgo clasifica su local según la matriz CENEPRED (D.S. N° 002-2018-PCM):</p>
      <ul>
        <li><strong>Riesgo Bajo / Medio (&lt; 500 m²):</strong> La inspección de seguridad se realiza de forma posterior a la entrega de la licencia.</li>
        <li><strong>Riesgo Alto / Muy Alto (≥ 500 m² o concurrencia masiva):</strong> Requiere inspección técnica previa con emisión de informe favorable antes de la licencia.</li>
      </ul>
    `;
  } else if (tipo === 'anexo4') {
    tit.textContent = 'Mesa de Ayuda — Declaración de Condiciones de Seguridad (Anexo 4)';
    body.innerHTML = `
      <p>El Anexo 4 certifica que el local cuenta con extintores operativos con carga vigente, luces de emergencia, señalización de evacuación y tablero eléctrico con llaves termomagnéticas y diferenciales.</p>
    `;
  }
  modal.style.display = 'flex';
}

function cerrarMesaAyuda() {
  document.getElementById('modalMesaAyuda').style.display = 'none';
}

function abrirModalSubsanacionCiudadana() {
  document.getElementById('modalSubsanarCiudadano').style.display = 'flex';
}

function cerrarModalSubsanarCiudadano() {
  document.getElementById('modalSubsanarCiudadano').style.display = 'none';
}

async function enviarSubsanacionCiudadana() {
  const detalle = document.getElementById('txtSubsanacionCiudadana').value.trim();
  if (!detalle) return alert('Por favor detalle los documentos subsanados.');

  const tramite = valor('inputBuscarTramite');
  try {
    const resExp = await fetch(`${API_EXPEDIENTES}/tramite/${tramite}`);
    if (!resExp.ok) throw new Error('Expediente no encontrado');
    const exp = await resExp.json();

    const res = await fetch(`${API_EXPEDIENTES}/${exp.id}/subsanar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ detalleSubsanacion: detalle })
    });

    if (!res.ok) throw new Error('Error al registrar subsanación');

    cerrarModalSubsanarCiudadano();
    alert('✅ Subsanación ingresada con éxito a la Mesa de Partes Virtual.');
    consultarTramite(tramite);
  } catch (err) {
    alert('❌ Error al subsanar: ' + err.message);
  }
}
