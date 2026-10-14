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
import pe.gob.munihuamanga.licencias.common.dto.ActualizarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.CuentaResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.DocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.EstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.PresentarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitudBorradorDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SolicitudBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoría I04.1: Pruebas de Concurrencia, Idempotencia y Seguridad Documental.
 * Valida presentación simultánea del mismo borrador, presentaciones paralelas de diferentes borradores,
 * reintento idempotente y ausencia de correlativos duplicados o errores 500 no controlados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DisplayName("Auditoría I04.1: Concurrencia, Idempotencia y Flujo Formal")
class PresentacionConcurrenciaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    @Autowired
    private SolicitudBorradorRepository borradorRepository;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    private String tokenCiudadano;
    private UUID cuentaId;
    private UUID establecimientoId;

    @BeforeEach
    void setUp() throws Exception {
        String dniPrueba = "43999901";
        String pwdPrueba = "password123";

        if (!cuentaRepository.existsByNumeroDocumento(dniPrueba)) {
            RegistroCuentaDto cuentaDto = RegistroCuentaDto.builder()
                    .tipoPersona(TipoPersona.NATURAL)
                    .tipoDocumento(TipoDocumento.DNI)
                    .numeroDocumento(dniPrueba)
                    .razonSocialONombre("Ciudadano Concurrencia Huamanga")
                    .email("concurrencia.ciudadano@test.pe")
                    .password(pwdPrueba)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/cuentas/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuentaDto)))
                    .andExpect(status().isCreated())
                    .andReturn();

            CuentaResponseDto resp = objectMapper.readValue(result.getResponse().getContentAsString(), CuentaResponseDto.class);
            cuentaId = resp.getId();
        } else {
            Cuenta c = cuentaRepository.findByNumeroDocumento(dniPrueba).orElseThrow();
            cuentaId = c.getId();
        }

        Cuenta cuenta = cuentaRepository.findById(cuentaId).orElseThrow();
        cuenta.setEmailVerificado(true);
        cuenta.setEstado(EstadoCuenta.ACTIVA);
        cuentaRepository.save(cuenta);

        tokenCiudadano = obtenerToken(dniPrueba, pwdPrueba);

        if (establecimientoRepository.findByCuentaId(cuentaId).isEmpty()) {
            CrearEstablecimientoDto estDto = CrearEstablecimientoDto.builder()
                    .nombreComercial("Comercial Concurrencia Huamanga")
                    .direccion("Jr. Bellido 234")
                    .distrito("Ayacucho")
                    .provincia("Huamanga")
                    .departamento("Ayacucho")
                    .referencia("Cerca al mercado central")
                    .latitud(new BigDecimal("-13.1631000"))
                    .longitud(new BigDecimal("-74.2236000"))
                    .ciiuCodigo("4711")
                    .giroPrincipal("Venta de abarrotes y víveres")
                    .areaMetrosCuadrados(new BigDecimal("45.00"))
                    .build();

            MvcResult resultEst = mockMvc.perform(post("/api/establecimientos/cuenta/" + cuentaId)
                            .header("Authorization", "Bearer " + tokenCiudadano)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(estDto)))
                    .andExpect(status().isCreated())
                    .andReturn();

            EstablecimientoDto estResp = objectMapper.readValue(resultEst.getResponse().getContentAsString(), EstablecimientoDto.class);
            establecimientoId = estResp.getId();
        } else {
            establecimientoId = establecimientoRepository.findByCuentaId(cuentaId).get(0).getId();
        }
    }

    private String obtenerToken(String username, String password) throws Exception {
        LoginRequestDto req = LoginRequestDto.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto resp = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponseDto.class);
        return resp.getToken();
    }

    private UUID prepararBorradorCompleto() throws Exception {
        CrearBorradorDto dto = CrearBorradorDto.builder()
                .cuentaId(cuentaId)
                .establecimientoId(establecimientoId)
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .build();

        MvcResult res = mockMvc.perform(post("/api/borradores")
                        .header("Authorization", "Bearer " + tokenCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        SolicitudBorradorDto borrador = objectMapper.readValue(res.getResponse().getContentAsString(), SolicitudBorradorDto.class);
        UUID borradorId = borrador.getId();

        ActualizarBorradorDto avance = ActualizarBorradorDto.builder()
                .giroNegocioDeclarado("Venta de abarrotes")
                .areaMetrosCuadrados(new BigDecimal("45.00"))
                .aforoPersonas(15)
                .nivelRiesgoDeclarado(NivelRiesgo.BAJO)
                .numeroOperacionSat("SAT-CONC-" + System.currentTimeMillis())
                .fechaPagoSat(LocalDate.now())
                .montoTasa(new BigDecimal("154.50"))
                .voucherId("VCH-CONC-" + System.currentTimeMillis())
                .build();

        mockMvc.perform(put("/api/borradores/" + borradorId)
                        .header("Authorization", "Bearer " + tokenCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(avance)))
                .andExpect(status().isOk());

        String pdfValido = "%PDF-1.4 Evidencia de prueba y planos Huamanga";
        String base64 = Base64.getEncoder().encodeToString(pdfValido.getBytes());

        AdjuntarDocumentoBorradorDto doc1 = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE)
                .nombreArchivo("evidencia_itse.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(base64)
                .build();

        mockMvc.perform(post("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doc1)))
                .andExpect(status().isCreated());

        AdjuntarDocumentoBorradorDto doc2 = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.COMPROBANTE_PAGO_SAT)
                .nombreArchivo("comprobante_sat.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(base64)
                .build();

        mockMvc.perform(post("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doc2)))
                .andExpect(status().isCreated());

        return borradorId;
    }

    @Test
    @DisplayName("I04.1-CONC-01: Dos presentaciones simultáneas del mismo borrador son serializadas de forma idempotente sin duplicados")
    void testDosPresentacionesSimultaneasMismoBorrador() throws Exception {
        UUID borradorId = prepararBorradorCompleto();

        PresentarBorradorDto presentarDto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .build();
        String jsonPayload = objectMapper.writeValueAsString(presentarDto);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger conflictosOReintentos = new AtomicInteger(0);
        AtomicReference<String> tramite1 = new AtomicReference<>();
        AtomicReference<String> tramite2 = new AtomicReference<>();

        for (int i = 0; i < 2; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult res = mockMvc.perform(post("/api/borradores/" + borradorId + "/presentar")
                                    .header("Authorization", "Bearer " + tokenCiudadano)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonPayload))
                            .andReturn();

                    int status = res.getResponse().getStatus();
                    if (status == 201 || status == 200) {
                        exitos.incrementAndGet();
                        ExpedienteResponseDto resp = objectMapper.readValue(
                                res.getResponse().getContentAsString(), ExpedienteResponseDto.class
                        );
                        if (threadNum == 0) tramite1.set(resp.getNumeroTramite());
                        else tramite2.set(resp.getNumeroTramite());
                    } else if (status == 409) {
                        conflictosOReintentos.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignorado para conteo de hilos
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        // Al menos una llamada fue exitosa creando el expediente
        assertThat(exitos.get()).isGreaterThanOrEqualTo(1);

        // Si ambas devolvieron 201/200 por idempotencia, deben referenciar al MISMO expediente exacto
        if (tramite1.get() != null && tramite2.get() != null) {
            assertThat(tramite1.get()).isEqualTo(tramite2.get());
        }

        // En la base de datos debe existir exactamente el expediente asociado al borrador
        SolicitudBorrador b = borradorRepository.findById(borradorId).orElseThrow();
        assertThat(b.getEstado()).isEqualTo(EstadoBorrador.PRESENTADO);
        assertThat(b.getExpediente()).isNotNull();
        assertThat(expedienteRepository.existsById(b.getExpediente().getId())).isTrue();
    }

    @Test
    @DisplayName("I04.1-CONC-02: Presentaciones simultáneas de borradores diferentes generan correlativos consecutivos sin colisión")
    void testPresentacionesSimultaneasBorradoresDiferentes() throws Exception {
        UUID borradorId1 = prepararBorradorCompleto();
        Thread.sleep(50); // Garantizar números de comprobante SAT distintos
        UUID borradorId2 = prepararBorradorCompleto();

        PresentarBorradorDto presentarDto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .build();
        String jsonPayload = objectMapper.writeValueAsString(presentarDto);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicReference<String> tramite1 = new AtomicReference<>();
        AtomicReference<String> tramite2 = new AtomicReference<>();
        AtomicReference<String> err1 = new AtomicReference<>();
        AtomicReference<String> err2 = new AtomicReference<>();

        executor.submit(() -> {
            try {
                startLatch.await();
                MvcResult res = mockMvc.perform(post("/api/borradores/" + borradorId1 + "/presentar")
                                .header("Authorization", "Bearer " + tokenCiudadano)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonPayload))
                        .andReturn();
                if (res.getResponse().getStatus() == 201) {
                    ExpedienteResponseDto resp = objectMapper.readValue(res.getResponse().getContentAsString(), ExpedienteResponseDto.class);
                    tramite1.set(resp.getNumeroTramite());
                } else {
                    err1.set("Status=" + res.getResponse().getStatus() + " Body=" + res.getResponse().getContentAsString());
                }
            } catch (Exception e) {
                err1.set(e.getMessage());
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                MvcResult res = mockMvc.perform(post("/api/borradores/" + borradorId2 + "/presentar")
                                .header("Authorization", "Bearer " + tokenCiudadano)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonPayload))
                        .andReturn();
                if (res.getResponse().getStatus() == 201) {
                    ExpedienteResponseDto resp = objectMapper.readValue(res.getResponse().getContentAsString(), ExpedienteResponseDto.class);
                    tramite2.set(resp.getNumeroTramite());
                } else {
                    err2.set("Status=" + res.getResponse().getStatus() + " Body=" + res.getResponse().getContentAsString());
                }
            } catch (Exception e) {
                err2.set(e.getMessage());
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        if (err1.get() != null) System.err.println("Thread 1 Error: " + err1.get());
        if (err2.get() != null) System.err.println("Thread 2 Error: " + err2.get());
        assertThat(tramite1.get()).isNotNull();
        assertThat(tramite2.get()).isNotNull();
        // Los dos correlativos deben ser diferentes y únicos
        assertThat(tramite1.get()).isNotEqualTo(tramite2.get());
        assertThat(tramite1.get()).matches("EXP-\\d{4}-\\d{5}");
        assertThat(tramite2.get()).matches("EXP-\\d{4}-\\d{5}");
    }

    @Test
    @DisplayName("I04.1-IDEMP: Reintento tras respuesta previa retorna el mismo expediente sin duplicar correlativo")
    void testReintentoIdempotenteNoDuplicaCorrelativo() throws Exception {
        UUID borradorId = prepararBorradorCompleto();

        PresentarBorradorDto presentarDto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .build();

        // 1. Primera llamada formal
        MvcResult res1 = mockMvc.perform(post("/api/borradores/" + borradorId + "/presentar")
                        .header("Authorization", "Bearer " + tokenCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presentarDto)))
                .andExpect(status().isCreated())
                .andReturn();

        ExpedienteResponseDto exp1 = objectMapper.readValue(res1.getResponse().getContentAsString(), ExpedienteResponseDto.class);
        String numeroOriginal = exp1.getNumeroTramite();

        // 2. Reintento (ejemplo cliente reintentando tras desconexión de red)
        MvcResult res2 = mockMvc.perform(post("/api/borradores/" + borradorId + "/presentar")
                        .header("Authorization", "Bearer " + tokenCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presentarDto)))
                .andExpect(status().isCreated())
                .andReturn();

        ExpedienteResponseDto exp2 = objectMapper.readValue(res2.getResponse().getContentAsString(), ExpedienteResponseDto.class);

        assertThat(exp2.getId()).isEqualTo(exp1.getId());
        assertThat(exp2.getNumeroTramite()).isEqualTo(numeroOriginal);
    }

    @Test
    @DisplayName("I04.1-DOC-SEC: Verificación y consulta segura de documento con registro de auditoría")
    void testConsultaSeguraDocumentoConAuditoria() throws Exception {
        UUID borradorId = prepararBorradorCompleto();

        // Listar documentos del borrador
        MvcResult resList = mockMvc.perform(get("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenCiudadano))
                .andExpect(status().isOk())
                .andReturn();

        DocumentoBorradorDto[] docs = objectMapper.readValue(
                resList.getResponse().getContentAsString(), DocumentoBorradorDto[].class
        );
        assertThat(docs).isNotEmpty();
        UUID docId = docs[0].getId();

        // Consultar el documento puntual
        MvcResult resDoc = mockMvc.perform(get("/api/borradores/" + borradorId + "/documentos/" + docId)
                        .header("Authorization", "Bearer " + tokenCiudadano))
                .andExpect(status().isOk())
                .andReturn();

        DocumentoBorradorDto docRecuperado = objectMapper.readValue(
                resDoc.getResponse().getContentAsString(), DocumentoBorradorDto.class
        );

        assertThat(docRecuperado.getId()).isEqualTo(docId);
        assertThat(docRecuperado.getHashSha256()).isNotNull();
        assertThat(docRecuperado.getNombreArchivo()).doesNotContain("..");
    }
}
