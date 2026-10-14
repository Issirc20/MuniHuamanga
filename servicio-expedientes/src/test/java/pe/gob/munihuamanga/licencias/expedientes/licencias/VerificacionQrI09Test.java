package pe.gob.munihuamanga.licencias.expedientes.licencias;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.gob.munihuamanga.licencias.common.dto.EmitirLicenciaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.LicenciaEmitida;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.AuditoriaConsultaInspeccionRepository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.service.RateLimiterService;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.model.SolicitudRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.repository.SolicitudRectificacionUbicacionRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.ResolucionAdministrativaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VerificacionQrI09Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LicenciaEmitidaRepository licenciaRepository;

    @Autowired
    private ResolucionAdministrativaRepository resolucionRepository;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    @Autowired
    private SolicitudRectificacionUbicacionRepository rectificacionRepository;

    @Autowired
    private AuditoriaConsultaInspeccionRepository auditoriaInspeccionRepository;

    @Autowired
    private InformeAdministrativoRepository informeAdministrativoRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    private Establecimiento ultimoEstablecimientoCreado;
    private Expediente ultimoExpedienteCreado;

    @BeforeEach
    void setUp() {
        rateLimiterService.reiniciarTodo();
    }

    private LicenciaEmitida crearLicenciaCompleta(String sufijo, EstadoLicencia estado, boolean conFirma, boolean temporalVencida) {
        Cuenta cuenta = Cuenta.builder()
                .id(UUID.randomUUID())
                .tipoPersona(pe.gob.munihuamanga.licencias.common.enums.TipoPersona.NATURAL)
                .tipoDocumento(pe.gob.munihuamanga.licencias.common.enums.TipoDocumento.DNI)
                .numeroDocumento("42" + sufijo + "99")
                .razonSocialONombre("TITULAR PRUEBA " + sufijo)
                .email("titular" + sufijo + "@test.gob.pe")
                .telefono("966123456")
                .estado(pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta.ACTIVA)
                .activo(true)
                .emailVerificado(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
        cuenta = cuentaRepository.save(cuenta);

        Establecimiento est = Establecimiento.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .codigoEstablecimiento("EST-" + sufijo)
                .nombreComercial("COMERCIAL HUAMANGA " + sufijo)
                .direccion("JR. MARISCAL CÁCERES " + sufijo + ", AYACUCHO")
                .distrito("Ayacucho")
                .provincia("Huamanga")
                .departamento("Ayacucho")
                .referencia("FRENTE AL PARQUE")
                .latitud(new BigDecimal("-13.1633333"))
                .longitud(new BigDecimal("-74.2238889"))
                .giroPrincipal("VENTA DE TEXTILES")
                .areaMetrosCuadrados(new BigDecimal("120.50"))
                .activo(true)
                .fechaRegistro(LocalDateTime.now())
                .build();
        est = establecimientoRepository.save(est);
        this.ultimoEstablecimientoCreado = est;

        Expediente exp = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-" + sufijo)
                .nombreTitular("TITULAR PRUEBA " + sufijo)
                .documentoIdentidad("42" + sufijo + "99")
                .nombreComercial("COMERCIAL HUAMANGA " + sufijo)
                .giroNegocio("VENTA DE TEXTILES")
                .direccionEstablecimiento("JR. MARISCAL CÁCERES " + sufijo + ", AYACUCHO")
                .solicitanteId(cuenta.getId())
                .establecimientoId(est.getId())
                .estado(EstadoExpediente.APROBADO)
                .areaMetrosCuadrados(new BigDecimal("120.50"))
                .aforoPersonas(45)
                .zonificacion("Comercial ZC")
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(15))
                .build();
        exp = expedienteRepository.save(exp);
        this.ultimoExpedienteCreado = exp;

        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(UUID.randomUUID())
                .expediente(exp)
                .numeroInforme("INF-2026-" + sufijo)
                .version(1)
                .evaluadorId(UUID.randomUUID())
                .evaluadorNombre("EVALUADOR PRUEBA")
                .fechaElaboracion(LocalDateTime.now())
                .conclusion(pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme.FAVORABLE)
                .estado(pe.gob.munihuamanga.licencias.common.enums.EstadoInforme.APROBADO)
                .fundamentosAdministrativos("Cumple con normas TUPA")
                .referenciasDocumentales("Anexo 1")
                .esVersionActual(true)
                .build();
        inf = informeAdministrativoRepository.save(inf);

        ResolucionAdministrativa res = ResolucionAdministrativa.builder()
                .id(UUID.randomUUID())
                .numeroResolucion("RES-2026-" + sufijo)
                .expediente(exp)
                .informeAdministrativo(inf)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .estado(conFirma ? EstadoResolucion.FIRMADA : EstadoResolucion.APROBADA_PENDIENTE_FIRMA)
                .fundamentos("Resolución favorable de prueba")
                .version(1)
                .versionInforme(1)
                .esVersionActual(true)
                .creadoPor("ADMIN")
                .fechaCreacion(LocalDateTime.now())
                .archivoPdf(conFirma ? new byte[]{0x25, 0x50, 0x44, 0x46} : null)
                .build();
        res = resolucionRepository.save(res);

        TipoVigenciaLicencia vigencia = temporalVencida ? TipoVigenciaLicencia.TEMPORAL : TipoVigenciaLicencia.INDETERMINADA;
        LocalDate fechaCaducidad = temporalVencida ? LocalDate.now().minusDays(10) : null;

        LicenciaEmitida lic = LicenciaEmitida.builder()
                .id(UUID.randomUUID())
                .numeroLicencia("LIC-2026-" + sufijo)
                .resolucion(res)
                .expediente(exp)
                .establecimiento(est)
                .codigoVerificacionQr("TOKEN-I09-" + sufijo)
                .titularTipoDocumento("DNI")
                .titularNumeroDocumento("42" + sufijo + "99")
                .titularNombreORazonSocial("TITULAR PRUEBA " + sufijo)
                .nombreComercial("COMERCIAL HUAMANGA " + sufijo)
                .direccionEstablecimiento("JR. MARISCAL CÁCERES " + sufijo + ", AYACUCHO")
                .giroActividad("VENTA DE TEXTILES")
                .ciiuCodigo("4751")
                .areaMetrosCuadrados(new BigDecimal("120.50"))
                .aforoPersonas(45)
                .zonificacion("Comercial ZC")
                .nivelRiesgoItse("BAJO")
                .tipoVigencia(vigencia)
                .fechaEmision(LocalDate.now().minusMonths(1))
                .fechaCaducidad(fechaCaducidad)
                .autoridadEmisora("Subgerencia de Comercio y Licencias")
                .estado(estado)
                .hashPdf("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .archivoPdf(conFirma ? new byte[]{0x25, 0x50, 0x44, 0x46} : null)
                .fechaCreacion(LocalDateTime.now())
                .creadoPor("ADMIN")
                .build();

        return licenciaRepository.save(lic);
    }

    @Test
    @DisplayName("I09-01: Verificación pública QR exitosa con minimización de datos y no-cache (Sin DNI)")
    void test01_verificacionPublicaQr_LicenciaVigente_Exito() throws Exception {
        String suf = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LicenciaEmitida lic = crearLicenciaCompleta(suf, EstadoLicencia.EMITIDA, true, false);

        mockMvc.perform(get("/api/licencias/verificar/" + lic.getCodigoVerificacionQr()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(jsonPath("$.codigoVerificacionQr").value(lic.getCodigoVerificacionQr()))
                .andExpect(jsonPath("$.numeroLicencia").value(lic.getNumeroLicencia()))
                .andExpect(jsonPath("$.nombreComercial").value(lic.getNombreComercial()))
                .andExpect(jsonPath("$.valida").value(true))
                .andExpect(jsonPath("$.firmaDigitalValida").value(true))
                .andExpect(jsonPath("$.titularDocumentoEnmascarado").doesNotExist())
                .andExpect(jsonPath("$.titularNumeroDocumento").doesNotExist());
    }

    @Test
    @DisplayName("I09-02: Verificación pública QR con token inexistente responde error controlado")
    void test02_verificacionPublicaQr_TokenInexistente_Error400() throws Exception {
        mockMvc.perform(get("/api/licencias/verificar/TOKEN-INEXISTENTE-999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("El código QR proporcionado no corresponde")));
    }

    @Test
    @DisplayName("I09-03: Verificación pública QR para Licencia REVOCADA refleja no vigencia")
    void test03_verificacionPublicaQr_LicenciaRevocada_Invalida() throws Exception {
        String suf = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LicenciaEmitida lic = crearLicenciaCompleta(suf, EstadoLicencia.REVOCADA, true, false);

        mockMvc.perform(get("/api/licencias/verificar/" + lic.getCodigoVerificacionQr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(false))
                .andExpect(jsonPath("$.estadoLicencia").value("REVOCADA"))
                .andExpect(jsonPath("$.mensajeVerificacion").value(containsString("REVOCADA")));
    }

    @Test
    @DisplayName("I09-04: Verificación pública QR para Licencia SUSPENDIDA refleja suspensión")
    void test04_verificacionPublicaQr_LicenciaSuspendida_Invalida() throws Exception {
        String suf = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LicenciaEmitida lic = crearLicenciaCompleta(suf, EstadoLicencia.SUSPENDIDA, true, false);

        mockMvc.perform(get("/api/licencias/verificar/" + lic.getCodigoVerificacionQr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(false))
                .andExpect(jsonPath("$.estadoLicencia").value("SUSPENDIDA"))
                .andExpect(jsonPath("$.mensajeVerificacion").value(containsString("SUSPENDIDA")));
    }

    @Test
    @DisplayName("I09-05: Verificación pública QR para Licencia TEMPORAL VENCIDA refleja caducidad")
    void test05_verificacionPublicaQr_LicenciaVencida_Invalida() throws Exception {
        String suf = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LicenciaEmitida lic = crearLicenciaCompleta(suf, EstadoLicencia.EMITIDA, true, true);

        mockMvc.perform(get("/api/licencias/verificar/" + lic.getCodigoVerificacionQr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(false))
                .andExpect(jsonPath("$.mensajeVerificacion").value(containsString("VENCIDA")));
    }

    @Test
    @DisplayName("I09-06: Verificación pública QR de licencia sin firma digital institucional rechaza validez")
    void test06_verificacionPublicaQr_ResolucionSinFirma_SinValidezOficial() throws Exception {
        String suf = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LicenciaEmitida lic = crearLicenciaCompleta(suf, EstadoLicencia.EMITIDA, false, false);

        mockMvc.perform(get("/api/licencias/verificar/" + lic.getCodigoVerificacionQr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(false))
                .andExpect(jsonPath("$.firmaDigitalValida").value(false))
                .andExpect(jsonPath("$.mensajeVerificacion").value(containsString("DOCUMENTO SIN VALIDEZ OFICIAL")));
    }

    @Test
    @DisplayName("I09-07: Consulta móvil de inspector con ROLE_INSPECTOR muestra georreferenciación y registra auditoría")
    @WithMockUser(username = "inspector_juan", roles = {"INSPECTOR"})
    void test07_inspectorConsultaMovil_InspectorAutenticado_Exito() throws Exception {
        String suf = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LicenciaEmitida lic = crearLicenciaCompleta(suf, EstadoLicencia.EMITIDA, true, false);

        mockMvc.perform(get("/api/inspector/licencias/verificar/" + lic.getCodigoVerificacionQr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoVerificacionQr").value(lic.getCodigoVerificacionQr()))
                .andExpect(jsonPath("$.titularNumeroDocumento").exists())
                .andExpect(jsonPath("$.coordenadasVerificadas").value(true))
                .andExpect(jsonPath("$.latitud").value(-13.1633333))
                .andExpect(jsonPath("$.longitud").value(-74.2238889))
                .andExpect(jsonPath("$.tipoCoordenada").value("DECLARADA_OFICIAL"))
                .andExpect(jsonPath("$.enlaceNavegacion").value(containsString("google.com/maps")))
                .andExpect(jsonPath("$.aforoPersonas").value(45))
                .andExpect(jsonPath("$.areaMetrosCuadrados").value(120.50));

        // Verificar que se haya registrado la consulta en auditoria_consultas_inspeccion
        var auditorias = auditoriaInspeccionRepository.findByLicenciaIdOrderByFechaConsultaDesc(lic.getId());
        assertFalse(auditorias.isEmpty());
        assertEquals("inspector_juan", auditorias.get(0).getInspectorUsername());
        assertEquals("CONSULTA_EXITOSA", auditorias.get(0).getResultado());
    }

    @Test
    @DisplayName("I09-08: Ciudadano autenticado recibe HTTP 403 Forbidden al intentar acceder al endpoint de inspector")
    @WithMockUser(username = "ciudadano_pedro", roles = {"CIUDADANO"})
    void test08_inspectorConsultaMovil_CiudadanoDenegado_Http403() throws Exception {
        mockMvc.perform(get("/api/inspector/licencias/verificar/TOKEN-CUALQUIERA"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I09-09: Consulta anónima sin autenticación a endpoint de inspector recibe HTTP 401 Unauthorized")
    void test09_inspectorConsultaMovil_AnonimoDenegado_Http401() throws Exception {
        mockMvc.perform(get("/api/inspector/licencias/verificar/TOKEN-CUALQUIERA"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("I09-10: Consulta de inspector para establecimiento con rectificación aprobada refleja RECTIFICADA_APROBADA")
    @WithMockUser(username = "inspector_juan", roles = {"INSPECTOR"})
    void test10_inspectorConsultaMovil_ConRectificacionAprobada_MuestraTipoRectificada() throws Exception {
        String suf = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LicenciaEmitida lic = crearLicenciaCompleta(suf, EstadoLicencia.EMITIDA, true, false);

        SolicitudRectificacionUbicacion rect = SolicitudRectificacionUbicacion.builder()
                .id(UUID.randomUUID())
                .establecimientoId(this.ultimoEstablecimientoCreado.getId())
                .expedienteId(this.ultimoExpedienteCreado.getId())
                .latitudOriginal(new BigDecimal("-13.1600000"))
                .longitudOriginal(new BigDecimal("-74.2200000"))
                .latitudPropuesta(new BigDecimal("-13.1633333"))
                .longitudPropuesta(new BigDecimal("-74.2238889"))
                .distanciaMetrosAprox(50.0)
                .justificacion("Ajuste topográfico aprobado")
                .solicitante("CIUDADANO")
                .estado(EstadoRectificacionUbicacion.APROBADA)
                .evaluador("ARQ_EVALUADOR")
                .fechaSolicitud(LocalDateTime.now().minusDays(5))
                .fechaEvaluacion(LocalDateTime.now().minusDays(2))
                .build();
        rectificacionRepository.save(rect);

        mockMvc.perform(get("/api/inspector/licencias/verificar/" + lic.getCodigoVerificacionQr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoCoordenada").value("RECTIFICADA_APROBADA"));
    }

    @Test
    @DisplayName("I09-11: Inspector no tiene permisos de emisión ni mutación administrativa (HTTP 403 Forbidden)")
    @WithMockUser(username = "inspector_juan", roles = {"INSPECTOR"})
    void test11_inspectorNoTienePermisoDeEdicionOEmision_Http403() throws Exception {
        EmitirLicenciaDto dto = EmitirLicenciaDto.builder()
                .resolucionId(UUID.randomUUID())
                .tipoVigencia(TipoVigenciaLicencia.INDETERMINADA)
                .build();

        mockMvc.perform(post("/api/licencias/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I09-12: Rate Limiter bloquea exceso de consultas públicas por IP con HTTP 429 Too Many Requests")
    void test12_rateLimiter_ExcesoPeticiones_Http429() throws Exception {
        String ipSimulada = "192.168.1.150";

        // Realizar 60 consultas válidas
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimiterService.admitirSolicitud(ipSimulada));
        }

        // La consulta 61 debe ser rechazada
        assertFalse(rateLimiterService.admitirSolicitud(ipSimulada));

        mockMvc.perform(get("/api/licencias/verificar/TOKEN-RATELIMIT")
                        .with(req -> {
                            req.setRemoteAddr(ipSimulada);
                            return req;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
