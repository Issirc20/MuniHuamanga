package pe.gob.munihuamanga.licencias.expedientes.documentos;

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
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoVersionadoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearObservacionDto;
import pe.gob.munihuamanga.licencias.common.dto.SubsanarObservacionesDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.ObservacionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.ObservacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ObservacionSubsanacionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private ObservacionExpedienteRepository observacionRepository;

    @Autowired
    private DocumentoVersionadoRepository documentoRepository;

    @Autowired
    private pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository operadorCuentaRepository;

    private Expediente expediente;
    private final String CIUDADANO_DNI = "43888801";
    private static final byte[] SAMPLE_PDF = "%PDF-1.4 test content for subsanacion".getBytes();
    private static final String SAMPLE_PDF_BASE64 = Base64.getEncoder().encodeToString(SAMPLE_PDF);

    @BeforeEach
    void setUp() {
        UUID cuentaId = UUID.randomUUID();
        Cuenta cuenta = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(pe.gob.munihuamanga.licencias.common.enums.TipoPersona.NATURAL)
                .tipoDocumento(pe.gob.munihuamanga.licencias.common.enums.TipoDocumento.DNI)
                .numeroDocumento(CIUDADANO_DNI)
                .razonSocialONombre("Juan Carlos Quispe")
                .email("juan.quispe@test.gob.pe")
                .emailVerificado(true)
                .activo(true)
                .estado(pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta.ACTIVA)
                .fechaCreacion(LocalDateTime.now())
                .build();
        cuentaRepository.save(cuenta);
        pe.gob.munihuamanga.licencias.expedientes.soporte.DatosDePrueba.vincularTitular(usuarioRepository, operadorCuentaRepository, cuenta, CIUDADANO_DNI, "Juan Carlos Quispe");

        expediente = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-77701")
                .solicitanteId(cuentaId)
                .cuentaId(cuentaId)
                .documentoIdentidad(CIUDADANO_DNI)
                .nombreTitular("Juan Carlos Quispe")
                .nombreComercial("Bodega El Sol")
                .giroNegocio("Abarrotes")
                .direccionEstablecimiento("Jr. 28 de Julio 450")
                .estado(EstadoExpediente.PRESENTADO)
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(21))
                .build();
        expediente = expedienteRepository.save(expediente);
    }

    @Test
    @WithMockUser(username = "evaluador_muni", roles = {"EVALUADOR"})
    @DisplayName("Evaluador formula observación y expediente transiciona a OBSERVADO")
    void formularObservaciones_evaluadorAutorizado_exito() throws Exception {
        CrearObservacionDto obsDto = CrearObservacionDto.builder()
                .requisitoAfectado("PLANO_DISTRIBUCION")
                .motivo("El plano adjunto no cuenta con escala legible ni firma técnica.")
                .plazoDiasHabiles(15)
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/observaciones", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(obsDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].requisitoAfectado").value("PLANO_DISTRIBUCION"))
                .andExpect(jsonPath("$[0].plazoDiasHabiles").value(15));

        Expediente actualizado = expedienteRepository.findById(expediente.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoExpediente.OBSERVADO);
        assertThat(actualizado.getMotivoObservacion()).contains("PLANO_DISTRIBUCION");
    }

    @Test
    @WithMockUser(username = CIUDADANO_DNI, roles = {"CIUDADANO"})
    @DisplayName("Ciudadano consulta observaciones de su expediente")
    void consultarObservaciones_ciudadanoPropietario_exito() throws Exception {
        ObservacionExpediente obs = ObservacionExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .requisitoAfectado("MEMORIA_DESCRIPTIVA")
                .motivo("Falta precisar el aforo por ambientes.")
                .fechaObservacion(LocalDateTime.now())
                .plazoDiasHabiles(15)
                .subsanada(false)
                .build();
        observacionRepository.save(obs);

        mockMvc.perform(get("/api/expedientes/{id}/observaciones", expediente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requisitoAfectado").value("MEMORIA_DESCRIPTIVA"))
                .andExpect(jsonPath("$[0].subsanada").value(false));
    }

    @Test
    @WithMockUser(username = "otro_ciudadano", roles = {"CIUDADANO"})
    @DisplayName("Ciudadano ajeno es denegado al consultar observaciones (BOLA/IDOR)")
    void consultarObservaciones_usuarioAjeno_denegado() throws Exception {
        mockMvc.perform(get("/api/expedientes/{id}/observaciones", expediente.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CIUDADANO_DNI, roles = {"CIUDADANO"})
    @DisplayName("Flujo completo de subsanación: adjuntar documento corregido y subsanar expediente")
    void flujoCompletoSubsanacion_conservaNumeroExpediente() throws Exception {
        // 1. Poner expediente en OBSERVADO con una observación pendiente
        org.springframework.test.util.ReflectionTestUtils.setField(expediente, "estado", EstadoExpediente.OBSERVADO);
        expedienteRepository.save(expediente);

        ObservacionExpediente obs = ObservacionExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .requisitoAfectado("PLANO_ARQUITECTURA")
                .motivo("Falta escala técnica")
                .fechaObservacion(LocalDateTime.now())
                .plazoDiasHabiles(15)
                .subsanada(false)
                .build();
        observacionRepository.save(obs);

        // 2. Ciudadano adjunta nueva versión del documento subsanado
        AdjuntarDocumentoVersionadoDto docDto = AdjuntarDocumentoVersionadoDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.PLANO_ARQUITECTURA)
                .nombreArchivo("plano_corregido.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(SAMPLE_PDF_BASE64)
                .motivoActualizacion("Subsanación de escala técnica")
                .observacionId(obs.getId())
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/documentos", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.esVersionActual").value(true));

        // 3. Ciudadano formaliza la subsanación
        SubsanarObservacionesDto subsDto = SubsanarObservacionesDto.builder()
                .detalleSubsanacion("Se ha adjuntado el plano con escala reglamentaria 1:50 y firma técnica.")
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/observaciones/subsanar", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subsDto)))
                .andExpect(status().isNoContent());

        // 4. Validar que el expediente conservó su ID y correlativo original
        Expediente subsanado = expedienteRepository.findById(expediente.getId()).orElseThrow();
        assertThat(subsanado.getId()).isEqualTo(expediente.getId());
        assertThat(subsanado.getNumeroTramite()).isEqualTo("EXP-2026-77701");
        assertThat(subsanado.getEstado()).isNotEqualTo(EstadoExpediente.OBSERVADO);

        // 5. Validar que la observación quedó marcada como subsanada
        ObservacionExpediente obsActualizada = observacionRepository.findById(obs.getId()).orElseThrow();
        assertThat(obsActualizada.getSubsanada()).isTrue();
        assertThat(obsActualizada.getDetalleSubsanacion()).contains("escala reglamentaria");
    }

    @Test
    @WithMockUser(username = "evaluador_muni", roles = {"EVALUADOR"})
    @DisplayName("I05.1: Formulación con TipoObservacion.RECEPCION_DOCUMENTAL asigna 2 días hábiles por defecto")
    void formularObservacion_tipoRecepcionDocumental_asignaDosDiasHabiles() throws Exception {
        CrearObservacionDto obsDto = CrearObservacionDto.builder()
                .requisitoAfectado("DNI_LEGIBLE")
                .motivo("Copia de documento de identidad ilegible en mesa de partes.")
                .tipoObservacion(pe.gob.munihuamanga.licencias.common.enums.TipoObservacion.RECEPCION_DOCUMENTAL)
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/observaciones", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(obsDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].tipoObservacion").value("RECEPCION_DOCUMENTAL"))
                .andExpect(jsonPath("$[0].plazoDiasHabiles").value(2))
                .andExpect(jsonPath("$[0].fundamentoLegal").value(org.hamcrest.Matchers.containsString("Art. 136.1")));
    }

    @Test
    @WithMockUser(username = CIUDADANO_DNI, roles = {"CIUDADANO"})
    @DisplayName("I05.1: Intento de subsanar dejando observaciones pendientes es rechazado")
    void subsanarObservaciones_parcialConPendientes_rechazado() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(expediente, "estado", EstadoExpediente.OBSERVADO);
        expedienteRepository.save(expediente);

        ObservacionExpediente obs1 = observacionRepository.save(ObservacionExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .requisitoAfectado("PLANO_1")
                .motivo("Falta escala")
                .fechaObservacion(LocalDateTime.now())
                .plazoDiasHabiles(5)
                .subsanada(false)
                .build());

        ObservacionExpediente obs2 = observacionRepository.save(ObservacionExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .requisitoAfectado("PLANO_2")
                .motivo("Falta firma")
                .fechaObservacion(LocalDateTime.now())
                .plazoDiasHabiles(5)
                .subsanada(false)
                .build());

        // Administrado intenta subsanar solo obs1 dejando obs2 pendiente
        SubsanarObservacionesDto subsDto = SubsanarObservacionesDto.builder()
                .detalleSubsanacion("Solo se subsanó plano 1")
                .observacionesIds(List.of(obs1.getId()))
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/observaciones/subsanar", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subsDto)))
                .andExpect(status().isBadRequest()); // Manages IllegalStateException -> HTTP 400
    }

    @Test
    @WithMockUser(username = "42567891", roles = {"CIUDADANO"})
    @DisplayName("I05.1: Ciudadano con mismo DNI no puede ver observaciones de expediente V1 sin reclamar formalmente")
    void consultarObservaciones_expedienteV1NoReclamado_denegado() throws Exception {
        Expediente expV1 = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2023-99999")
                .solicitanteId(UUID.randomUUID())
                .cuentaId(null)
                .documentoIdentidad("42567891")
                .nombreTitular("Titular V1 Historico")
                .nombreComercial("Comercio Historico V1")
                .giroNegocio("Abarrotes")
                .direccionEstablecimiento("Jr. Callao 123")
                .estado(EstadoExpediente.OBSERVADO)
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(21))
                .build();
        expV1 = expedienteRepository.save(expV1);

        mockMvc.perform(get("/api/expedientes/{id}/observaciones", expV1.getId()))
                .andExpect(status().isForbidden());
    }
}
