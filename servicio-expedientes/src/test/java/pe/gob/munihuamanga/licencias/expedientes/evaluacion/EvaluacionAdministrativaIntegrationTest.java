package pe.gob.munihuamanga.licencias.expedientes.evaluacion;

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
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.AprobarInformeDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearInformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.EvaluarRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.dto.RevisarInformeDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitarRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Anexo4Condiciones;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.PagoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.PagoExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.model.SolicitudRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.repository.SolicitudRectificacionUbicacionRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.model.EvaluacionZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.repository.EvaluacionZonificacionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EvaluacionAdministrativaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    @Autowired
    private DocumentoVersionadoRepository documentoVersionadoRepository;

    @Autowired
    private PagoExpedienteRepository pagoExpedienteRepository;

    @Autowired
    private EvaluacionZonificacionRepository evaluacionZonificacionRepository;

    @Autowired
    private InformeAdministrativoRepository informeAdministrativoRepository;

    @Autowired
    private SolicitudRectificacionUbicacionRepository solicitudRectificacionRepository;

    private Expediente expedientePrueba;
    private Establecimiento establecimientoPrueba;

    @BeforeEach
    void setUp() {
        long rnd = System.nanoTime() % 100000000L;
        UUID cuentaId = UUID.randomUUID();
        Cuenta cuenta = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(String.format("%08d", rnd))
                .razonSocialONombre("Mario Vargas Llosa")
                .email("mario.vargas@gmail.com")
                .fechaCreacion(LocalDateTime.now())
                .build();
        cuentaRepository.save(cuenta);

        UUID estId = UUID.randomUUID();
        establecimientoPrueba = Establecimiento.builder()
                .id(estId)
                .cuenta(cuenta)
                .codigoEstablecimiento("EST-" + rnd)
                .nombreComercial("Librería Huamanga")
                .direccion("Portal Constitución 45")
                .distrito("Ayacucho")
                .provincia("Huamanga")
                .departamento("Ayacucho")
                .latitud(new BigDecimal("-13.1633000"))
                .longitud(new BigDecimal("-74.2238000"))
                .giroPrincipal("Venta de libros y revistas")
                .areaMetrosCuadrados(new BigDecimal("85.50"))
                .activo(true)
                .fechaRegistro(LocalDateTime.now())
                .build();
        establecimientoRepository.save(establecimientoPrueba);

        expedientePrueba = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-" + rnd)
                .solicitanteId(cuentaId)
                .cuentaId(cuentaId)
                .establecimientoId(estId)
                .nombreTitular("Mario Vargas Llosa")
                .documentoIdentidad(String.format("%08d", rnd))
                .nombreComercial("Librería Huamanga")
                .giroNegocio("Venta de libros y revistas")
                .direccionEstablecimiento("Portal Constitución 45")
                .estado(EstadoExpediente.PRESENTADO)
                .nivelRiesgo(NivelRiesgo.BAJO)
                .montoTasa(new BigDecimal("154.50"))
                .anexo4Condiciones(new Anexo4Condiciones())
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(10))
                .build();
        expedienteRepository.save(expedientePrueba);
    }

    @Test
    @DisplayName("GET /bandeja -> 200 OK para EVALUADOR")
    @WithMockUser(username = "evaluador_muni", roles = {"EVALUADOR"})
    void listarBandeja_evaluador_retorna200() throws Exception {
        mockMvc.perform(get("/api/evaluaciones-administrativas/bandeja")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /bandeja -> 403 Forbidden para CIUDADANO")
    @WithMockUser(username = "ciudadano_juan", roles = {"CIUDADANO"})
    void listarBandeja_ciudadano_retorna403() throws Exception {
        mockMvc.perform(get("/api/evaluaciones-administrativas/bandeja")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /expedientes/{id}/checklist -> 200 OK con items evaluados")
    @WithMockUser(username = "evaluador_muni", roles = {"EVALUADOR"})
    void obtenerChecklist_evaluador_retorna200() throws Exception {
        mockMvc.perform(get("/api/evaluaciones-administrativas/expedientes/{id}/checklist", expedientePrueba.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expedienteId").value(expedientePrueba.getId().toString()))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("POST /informes con conclusión FAVORABLE y requisitos completos -> 201 Created")
    @WithMockUser(username = "evaluador_muni", roles = {"EVALUADOR"})
    void elaborarInforme_requisitosCompletos_retorna201() throws Exception {
        // Preparar requisitos para checklist apto
        DocumentoVersionado doc = DocumentoVersionado.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedientePrueba.getId())
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .version(1)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .nombreArchivo("anexo1.pdf")
                .cargadoPor(UUID.randomUUID())
                .cargadoPorNombre("Usuario Prueba")
                .estadoRevision(EstadoRevisionDocumento.CONFORME)
                .fechaCarga(LocalDateTime.now())
                .build();
        documentoVersionadoRepository.save(doc);

        PagoExpediente pago = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedientePrueba.getId())
                .codigoComprobante("REC-2026-99001")
                .montoDeclarado(new BigDecimal("154.50"))
                .montoValidado(new BigDecimal("154.50"))
                .fechaDeclarada(LocalDate.now())
                .estado(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .validadorNombre("Cajero")
                .numeroOperacionSat("SAT-12345")
                .fechaRegistro(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        pagoExpedienteRepository.save(pago);

        EvaluacionZonificacion zon = EvaluacionZonificacion.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedientePrueba.getId())
                .direccionDeclarada("Portal Constitución 45")
                .giroComercial("Venta de libros")
                .referenciaZonificacion("Zona Comercial Central")
                .fuenteSustentoTecnico("Informe Técnico N° 099-2026-MPH")
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .fechaRegistro(LocalDateTime.now())
                .build();
        evaluacionZonificacionRepository.save(zon);

        CrearInformeAdministrativoDto dto = CrearInformeAdministrativoDto.builder()
                .expedienteId(expedientePrueba.getId())
                .conclusion(ConclusionInforme.FAVORABLE)
                .fundamentosAdministrativos("Se verificaron todos los requisitos del TUPA vigente de Huamanga.")
                .referenciasDocumentales("Anexo 1 conforme, pago SAT y zonificación compatibles.")
                .observaciones("Ninguna observación.")
                .build();

        mockMvc.perform(post("/api/evaluaciones-administrativas/informes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.conclusion").value("FAVORABLE"))
                .andExpect(jsonPath("$.numeroInforme").isNotEmpty());
    }

    @Test
    @DisplayName("Flujo completo: BORRADOR -> EN_REVISION -> VISADO -> APROBADO con APTO_PARA_RESOLUCION")
    @WithMockUser(username = "admin_master", roles = {"ADMIN"})
    void flujoCompleto_informeAprobado_habilitaAptoParaResolucion() throws Exception {
        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(UUID.randomUUID())
                .expediente(expedientePrueba)
                .numeroInforme("INF-2026-99001")
                .version(1)
                .evaluadorId(UUID.nameUUIDFromBytes("otro_evaluador".getBytes()))
                .evaluadorNombre("otro_evaluador")
                .conclusion(ConclusionInforme.FAVORABLE)
                .estado(EstadoInforme.VISADO)
                .fechaElaboracion(LocalDateTime.now())
                .fundamentosAdministrativos("Fundamentos completos y fundamentados.")
                .referenciasDocumentales("Referencias documentales.")
                .esVersionActual(true)
                .build();
        informeAdministrativoRepository.save(inf);

        // Cumplir requisitos de checklist
        DocumentoVersionado doc = DocumentoVersionado.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedientePrueba.getId())
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .version(1)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .nombreArchivo("anexo1.pdf")
                .cargadoPor(UUID.randomUUID())
                .cargadoPorNombre("Usuario Prueba")
                .estadoRevision(EstadoRevisionDocumento.CONFORME)
                .fechaCarga(LocalDateTime.now())
                .build();
        documentoVersionadoRepository.save(doc);

        PagoExpediente pago = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedientePrueba.getId())
                .codigoComprobante("REC-2026-99002")
                .montoDeclarado(new BigDecimal("154.50"))
                .montoValidado(new BigDecimal("154.50"))
                .fechaDeclarada(LocalDate.now())
                .estado(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .validadorNombre("Cajero")
                .numeroOperacionSat("SAT-12345")
                .fechaRegistro(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        pagoExpedienteRepository.save(pago);

        EvaluacionZonificacion zon = EvaluacionZonificacion.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedientePrueba.getId())
                .direccionDeclarada("Portal Constitución 45")
                .giroComercial("Venta de libros")
                .referenciaZonificacion("Zona Comercial Central")
                .fuenteSustentoTecnico("Informe Técnico N° 099-2026-MPH")
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .fechaRegistro(LocalDateTime.now())
                .build();
        evaluacionZonificacionRepository.save(zon);

        AprobarInformeDto aprobarDto = AprobarInformeDto.builder()
                .observacionesAprobacion("Aprobación formal de gerencia habilitando emisión de resolución.")
                .build();

        mockMvc.perform(post("/api/evaluaciones-administrativas/informes/{id}/aprobar", inf.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aprobarDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        Expediente expActualizado = expedienteRepository.findById(expedientePrueba.getId()).orElseThrow();
        assertThat(expActualizado.getEstado()).isEqualTo(EstadoExpediente.APTO_PARA_RESOLUCION);
    }

    @Test
    @DisplayName("GET /pdf-preliminar -> 200 OK con Content-Type application/pdf")
    @WithMockUser(username = "evaluador_muni", roles = {"EVALUADOR"})
    void descargarPdfPreliminar_retornaPdf() throws Exception {
        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(UUID.randomUUID())
                .expediente(expedientePrueba)
                .numeroInforme("INF-2026-88001")
                .version(1)
                .evaluadorId(UUID.randomUUID())
                .evaluadorNombre("evaluador_muni")
                .conclusion(ConclusionInforme.FAVORABLE)
                .estado(EstadoInforme.BORRADOR)
                .fechaElaboracion(LocalDateTime.now())
                .fundamentosAdministrativos("Fundamentos completos y fundamentados.")
                .referenciasDocumentales("Referencias documentales.")
                .esVersionActual(true)
                .build();
        informeAdministrativoRepository.save(inf);

        mockMvc.perform(get("/api/evaluaciones-administrativas/informes/{id}/pdf-preliminar", inf.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("POST /rectificaciones-ubicacion -> 201 Created para ciudadano autenticado")
    @WithMockUser(username = "mario.vargas@gmail.com", roles = {"CIUDADANO"})
    void solicitarRectificacion_ciudadano_retorna201() throws Exception {
        SolicitarRectificacionUbicacionDto dto = SolicitarRectificacionUbicacionDto.builder()
                .establecimientoId(establecimientoPrueba.getId())
                .latitudPropuesta(new BigDecimal("-13.1633500"))
                .longitudPropuesta(new BigDecimal("-74.2238500"))
                .justificacion("Ajuste por precisión de GPS en puerta principal del establecimiento.")
                .build();

        mockMvc.perform(post("/api/rectificaciones-ubicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.latitudOriginal").value(establecimientoPrueba.getLatitud().doubleValue()));
    }

    @Test
    @DisplayName("POST /rectificaciones-ubicacion -> 403 Forbidden para ciudadano ajeno al establecimiento (Seguridad BOLA/IDOR)")
    @WithMockUser(username = "hacker.ajeno@gmail.com", roles = {"CIUDADANO"})
    void solicitarRectificacion_ciudadanoAjeno_retorna403() throws Exception {
        SolicitarRectificacionUbicacionDto dto = SolicitarRectificacionUbicacionDto.builder()
                .establecimientoId(establecimientoPrueba.getId())
                .latitudPropuesta(new BigDecimal("-13.1633500"))
                .longitudPropuesta(new BigDecimal("-74.2238500"))
                .justificacion("Intento no autorizado de rectificar coordenadas de establecimiento ajeno.")
                .build();

        mockMvc.perform(post("/api/rectificaciones-ubicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /rectificaciones-ubicacion/{id}/evaluar -> 200 OK para EVALUADOR")
    @WithMockUser(username = "evaluador_muni", roles = {"EVALUADOR"})
    void evaluarRectificacion_evaluador_retorna200() throws Exception {
        SolicitudRectificacionUbicacion sol = SolicitudRectificacionUbicacion.builder()
                .id(UUID.randomUUID())
                .establecimientoId(establecimientoPrueba.getId())
                .latitudOriginal(establecimientoPrueba.getLatitud())
                .longitudOriginal(establecimientoPrueba.getLongitud())
                .latitudPropuesta(new BigDecimal("-13.1633500"))
                .longitudPropuesta(new BigDecimal("-74.2238500"))
                .justificacion("Ajuste de GPS en puerta")
                .estado(pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion.PENDIENTE)
                .solicitante("mario.vargas@gmail.com")
                .fechaSolicitud(LocalDateTime.now())
                .build();
        solicitudRectificacionRepository.save(sol);

        EvaluarRectificacionUbicacionDto evalDto = EvaluarRectificacionUbicacionDto.builder()
                .aprobado(true)
                .motivoRespuesta("Verificado conforme con cartografía digital municipal.")
                .esTrasladoFisico(false)
                .build();

        mockMvc.perform(post("/api/rectificaciones-ubicacion/{id}/evaluar", sol.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(evalDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }
}
