package pe.gob.munihuamanga.licencias.expedientes.borradores;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.EstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.PresentarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.ReclamarExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolverReclamacionDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitudBorradorDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.expedientes.config.UsuarioDataInitializer;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pe.gob.munihuamanga.licencias.expedientes.soporte.DatosDePrueba.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class BorradorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    private String tokenCiudadanoTitular;
    private String tokenCiudadanoTercero;
    private String tokenEvaluador;
    private String tokenSupervisor;

    private final UUID cuentaTitularId = UsuarioDataInitializer.CUENTA_MARIA_QUISPE;
    private UUID establecimientoId;

    @BeforeEach
    void setUp() throws Exception {
        tokenCiudadanoTitular = obtenerToken(mockMvc, objectMapper, CIUDADANA_TITULAR, CLAVE_CIUDADANO);
        tokenCiudadanoTercero = obtenerToken(mockMvc, objectMapper, CIUDADANO_TERCERO, CLAVE_CIUDADANO);
        tokenEvaluador = obtenerToken(mockMvc, objectMapper, EVALUADOR, CLAVE_EVALUADOR);
        tokenSupervisor = obtenerToken(mockMvc, objectMapper, SUPERVISOR, CLAVE_SUPERVISOR);

        // Registrar un establecimiento para el titular (la cuenta semilla ya está activa y verificada)
        CrearEstablecimientoDto estDto = CrearEstablecimientoDto.builder()
                .nombreComercial("Boutique Artesanal Huamanga")
                .direccion("Jr. 9 de Diciembre 142")
                .distrito("Ayacucho")
                .provincia("Huamanga")
                .departamento("Ayacucho")
                .referencia("A media cuadra de la Plaza Mayor")
                .latitud(new BigDecimal("-13.1631000"))
                .longitud(new BigDecimal("-74.2236000"))
                .ciiuCodigo("4773")
                .giroPrincipal("Venta de retablos y souvenirs")
                .areaMetrosCuadrados(new BigDecimal("35.50"))
                .build();

        MvcResult resEst = mockMvc.perform(post("/api/establecimientos/cuenta/" + cuentaTitularId)
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(estDto)))
                .andExpect(status().isCreated())
                .andReturn();

        EstablecimientoDto eResp = objectMapper.readValue(resEst.getResponse().getContentAsString(), EstablecimientoDto.class);
        establecimientoId = eResp.getId();
    }

    @Test
    @DisplayName("I04: Flujo integral de borrador - Crear, guardar avances, adjuntar y presentar con número EXP-* único")
    void flujoCompletoBorradorYPresentacionFormal() throws Exception {
        // 1. Crear Borrador
        CrearBorradorDto crearDto = CrearBorradorDto.builder()
                .cuentaId(cuentaTitularId)
                .establecimientoId(establecimientoId)
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nombreComercialDeclarado("Boutique Artesanal Huamanga")
                .giroNegocioDeclarado("Venta de artesanías y retablos")
                .areaMetrosCuadrados(new BigDecimal("35.50"))
                .build();

        MvcResult resBorrador = mockMvc.perform(post("/api/borradores")
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_EDICION"))
                .andExpect(jsonPath("$.numeroTramiteFormal").doesNotExist()) // NO genera número formal EXP-*
                .andReturn();

        SolicitudBorradorDto borradorCreado = objectMapper.readValue(resBorrador.getResponse().getContentAsString(), SolicitudBorradorDto.class);
        UUID borradorId = borradorCreado.getId();

        // 2. Guardar avances parciales
        ActualizarBorradorDto avanceDto = ActualizarBorradorDto.builder()
                .actividadDetallada("Comercio al por menor de retablos y cerámicas tradicionales")
                .aforoPersonas(15)
                .numeroPisos(1)
                .horarioInicio("08:00")
                .horarioFin("20:00")
                .telefonoContacto("966112233")
                .correoNotificacion("maria.quispe@test.pe")
                .autorizaNotificacion(true)
                .nivelRiesgoDeclarado(NivelRiesgo.BAJO)
                .numeroOperacionSat("OP-SAT-2026-8877")
                .fechaPagoSat(LocalDate.now())
                .montoTasa(new BigDecimal("154.50"))
                .voucherId("VCH-2026-8877")
                .build();

        mockMvc.perform(put("/api/borradores/" + borradorId)
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(avanceDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aforoPersonas").value(15))
                .andExpect(jsonPath("$.numeroOperacionSat").value("OP-SAT-2026-8877"));

        // 3. Adjuntar documento de riesgo ITSE
        String pdfContenido = "%PDF-1.4 Evidencia de clasificación ITSE Nivel Bajo";
        String base64Pdf = Base64.getEncoder().encodeToString(pdfContenido.getBytes());

        AdjuntarDocumentoBorradorDto docItse = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE)
                .nombreArchivo("evidencia_itse_bajo.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(base64Pdf)
                .descripcion("Evidencia fotográfica de cumplimiento de condiciones de seguridad")
                .build();

        mockMvc.perform(post("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docItse)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hashSha256").isNotEmpty())
                .andExpect(jsonPath("$.tipoDocumento").value("EVIDENCIA_RIESGO_ITSE"));

        // 4. Adjuntar comprobante de pago SAT
        AdjuntarDocumentoBorradorDto docSat = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.COMPROBANTE_PAGO_SAT)
                .nombreArchivo("voucher_sat_8877.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(base64Pdf)
                .descripcion("Voucher de pago SAT en ventanilla")
                .build();

        mockMvc.perform(post("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docSat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisoLegal").isNotEmpty());

        // 5. Validar requisitos pendientes
        mockMvc.perform(get("/api/borradores/" + borradorId + "/requisitos-pendientes")
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aptoParaPresentacion").value(true));

        // 6. Protección BOLA/IDOR: Ciudadano tercero no puede ver ni modificar el borrador
        mockMvc.perform(get("/api/borradores/" + borradorId)
                        .header("Authorization", "Bearer " + tokenCiudadanoTercero))
                .andExpect(status().isForbidden());

        // 7. Presentación formal transaccional
        PresentarBorradorDto presentarDto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .comentarios("Formalización de solicitud de licencia para boutique artesanal")
                .build();

        MvcResult resFormal = mockMvc.perform(post("/api/borradores/" + borradorId + "/presentar")
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presentarDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroTramite").isNotEmpty())
                .andExpect(jsonPath("$.estado").value("PRESENTADO"))
                .andReturn();

        ExpedienteResponseDto formal = objectMapper.readValue(resFormal.getResponse().getContentAsString(), ExpedienteResponseDto.class);
        String numeroGenerado = formal.getNumeroTramite();
        assertThat(numeroGenerado).startsWith("EXP-2026-");

        // 8. Reintento Idempotente: Debe retornar el mismo expediente sin crear duplicados
        mockMvc.perform(post("/api/borradores/" + borradorId + "/presentar")
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presentarDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroTramite").value(numeroGenerado));
    }

    @Test
    @DisplayName("I04: Procedimiento de Reclamación y Validación de Titularidad de expediente histórico")
    void procedimientoReclamacionTitularidadHistorica() throws Exception {
        // Expediente histórico sin cuenta vinculada (p. ej. recibido presencialmente antes de la migración);
        // conserva la identidad original del solicitante, pero aún no está enlazado a una cuenta del portal.
        Expediente hist = expedienteRepository.save(Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-HIST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .solicitanteId(UUID.randomUUID())
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad(CIUDADANA_TITULAR)
                .nombreComercial("Boutique Artesanal Huamanga (histórico)")
                .giroNegocio("Venta de artesanías y retablos")
                .direccionEstablecimiento("Jr. 9 de Diciembre 142")
                .estado(EstadoExpediente.PRESENTADO)
                .fechaCreacion(LocalDateTime.now())
                .build());

        // 1. Administrado solicita reclamación aportando evidencia
        ReclamarExpedienteDto reclamoDto = ReclamarExpedienteDto.builder()
                .cuentaId(cuentaTitularId)
                .evidenciaTitularidad("Cargo de recepción presencial en Mesa de Partes N° 004812")
                .motivo("Vincular expediente histórico de mi boutique a mi cuenta virtual")
                .build();

        MvcResult resReclamo = mockMvc.perform(post("/api/expedientes/" + hist.getId() + "/reclamar")
                        .header("Authorization", "Bearer " + tokenCiudadanoTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reclamoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();

        UUID reclamacionId = UUID.fromString(objectMapper.readTree(resReclamo.getResponse().getContentAsString()).get("id").asText());

        // 2. Funcionario municipal consulta bandeja de pendientes
        mockMvc.perform(get("/api/expedientes/reclamaciones/pendientes")
                        .header("Authorization", "Bearer " + tokenEvaluador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 3. Funcionario municipal resuelve favorablemente la reclamación
        ResolverReclamacionDto resolverDto = ResolverReclamacionDto.builder()
                .aprobado(true)
                .motivoResolucion("Verificado y cotejado con el archivador físico de la Gerencia")
                .build();

        mockMvc.perform(put("/api/expedientes/reclamaciones/" + reclamacionId + "/resolver")
                        .header("Authorization", "Bearer " + tokenSupervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolverDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        // 4. Verificar que el expediente histórico ahora está formalmente vinculado a la cuenta
        Expediente histActualizado = expedienteRepository.findById(hist.getId()).orElseThrow();
        assertThat(histActualizado.getCuentaId()).isEqualTo(cuentaTitularId);
    }
}
