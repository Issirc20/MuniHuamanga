package pe.gob.munihuamanga.licencias.expedientes.pagos;

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
import pe.gob.munihuamanga.licencias.common.dto.DeclararPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.RechazarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistrarEvaluacionZonificacionDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarPagoManualDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.PagoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.PagoExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class PagoZonificacionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private PagoExpedienteRepository pagoRepository;

    @Autowired
    private pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository operadorCuentaRepository;

    private final String CIUDADANO_DNI = "47889911";
    private UUID cuentaId;
    private Expediente expediente;

    @BeforeEach
    void setUp() {
        cuentaId = UUID.randomUUID();
        Cuenta cuenta = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(CIUDADANO_DNI)
                .razonSocialONombre("Carlos Mendoza Prado")
                .email("carlos.mendoza@test.pe")
                .emailVerificado(true)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
        cuentaRepository.save(cuenta);
        pe.gob.munihuamanga.licencias.expedientes.soporte.DatosDePrueba.vincularTitular(usuarioRepository, operadorCuentaRepository, cuenta, CIUDADANO_DNI, "Carlos Mendoza Prado");

        expediente = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-66778")
                .solicitanteId(cuentaId)
                .cuentaId(cuentaId)
                .documentoIdentidad(CIUDADANO_DNI)
                .nombreTitular("Carlos Mendoza Prado")
                .nombreComercial("Librería Huamanga")
                .giroNegocio("Venta de libros y útiles de oficina")
                .direccionEstablecimiento("Portal Unión 15")
                .estado(EstadoExpediente.PENDIENTE_PAGO)
                .nivelRiesgo(NivelRiesgo.BAJO)
                .montoTasa(new BigDecimal("154.50"))
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(21))
                .build();
        expediente = expedienteRepository.save(expediente);
    }

    @Test
    @WithMockUser(username = CIUDADANO_DNI, roles = {"CIUDADANO"})
    @DisplayName("El administrado declara el comprobante de la tasa liquidada; queda pendiente de validación")
    void declararPago_ciudadanoPropietario_retorna201() throws Exception {
        DeclararPagoDto dto = DeclararPagoDto.builder()
                .codigoComprobante("VCH-SAT-2026-0001")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .observacionesAdministrado("Cancelado en ventanilla SAT central")
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/pagos/declarar", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoComprobante").value("VCH-SAT-2026-0001"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_VALIDACION"))
                .andExpect(jsonPath("$.montoDeclarado").value(154.50));

        Expediente expActual = expedienteRepository.findById(expediente.getId()).orElseThrow();
        assertThat(expActual.getEstado()).isEqualTo(EstadoExpediente.PENDIENTE_PAGO);
    }

    @Test
    @WithMockUser(username = "cajero_pedro", roles = {"CAJERO"})
    @DisplayName("El cajero valida el comprobante y el expediente pasa a evaluación, iniciando el plazo legal")
    void validarPagoManual_cajero_retorna200() throws Exception {
        PagoExpediente pago = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .codigoComprobante("VCH-SAT-2026-0002")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .tasaTupaEsperada(new BigDecimal("154.50"))
                .estado(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .fechaRegistro(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        pago = pagoRepository.save(pago);

        ValidarPagoManualDto dto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-SAT-554433")
                .montoValidado(new BigDecimal("154.50"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Cotejo con registro diario de caja ventanilla 1")
                .build();

        mockMvc.perform(post("/api/pagos/{id}/validar", pago.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDADO_MANUALMENTE"))
                .andExpect(jsonPath("$.numeroOperacionSat").value("OP-SAT-554433"))
                .andExpect(jsonPath("$.validadorNombre").value("cajero_pedro"));

        Expediente expActual = expedienteRepository.findById(expediente.getId()).orElseThrow();
        // Con el pago validado el expediente queda completo y pasa a evaluación
        assertThat(expActual.getEstado()).isEqualTo(EstadoExpediente.EN_EVALUACION);
        assertThat(expActual.getFechaLimite()).isNotNull();
        assertThat(expActual.getVoucherId()).isEqualTo("VCH-SAT-2026-0002");
    }

    @Test
    @WithMockUser(username = "cajero_pedro", roles = {"CAJERO"})
    @DisplayName("I06.1 Concurrencia: Revalidación de pago ya validado es bloqueada con HTTP 400")
    void validarPagoManual_dobleValidacion_bloqueada() throws Exception {
        PagoExpediente pago = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .codigoComprobante("VCH-SAT-YA-VALIDADO")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .tasaTupaEsperada(new BigDecimal("154.50"))
                .estado(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .fechaRegistro(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        pago = pagoRepository.save(pago);

        ValidarPagoManualDto dto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-SAT-DUP")
                .montoValidado(new BigDecimal("154.50"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Intento de revalidación simultánea")
                .build();

        mockMvc.perform(post("/api/pagos/{id}/validar", pago.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ya fue validado manualmente con anterioridad")));
    }

    @Test
    @WithMockUser(username = "cajero_pedro", roles = {"CAJERO"})
    @DisplayName("I06.1 Integridad: Validación de pago RECHAZADO es bloqueada con HTTP 400")
    void validarPagoManual_pagoRechazado_bloqueado() throws Exception {
        PagoExpediente pago = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .codigoComprobante("VCH-SAT-ANULADO")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .tasaTupaEsperada(new BigDecimal("154.50"))
                .estado(EstadoValidacionPago.RECHAZADO)
                .fechaRegistro(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        pago = pagoRepository.save(pago);

        ValidarPagoManualDto dto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-SAT-RECH")
                .montoValidado(new BigDecimal("154.50"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Intento de validar voucher cancelado")
                .build();

        mockMvc.perform(post("/api/pagos/{id}/validar", pago.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ha sido RECHAZADO")));
    }

    @Test
    @WithMockUser(username = CIUDADANO_DNI, roles = {"CIUDADANO"})
    @DisplayName("I06.1 Normalización: Comprobante con espacios redundantes es normalizado correctamente")
    void declararPago_normalizacionEspacios_exito() throws Exception {
        DeclararPagoDto dto = DeclararPagoDto.builder()
                .codigoComprobante("  vch - sat - 2026 - 7788  ")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .observacionesAdministrado("Voucher con formato normalizable")
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/pagos/declarar", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoComprobante").value("VCH-SAT-2026-7788"));
    }

    @Test
    @WithMockUser(username = CIUDADANO_DNI, roles = {"CIUDADANO"})
    @DisplayName("I06 Seguridad: Ciudadano no puede validar manualmente su propio pago (403 Forbidden)")
    void validarPagoManual_ciudadano_retorna403() throws Exception {
        UUID pagoId = UUID.randomUUID();

        ValidarPagoManualDto dto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-FALSA-999")
                .montoValidado(new BigDecimal("154.50"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Auto-validación ciudadana denegada")
                .build();

        mockMvc.perform(post("/api/pagos/{id}/validar", pagoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CIUDADANO_DNI, roles = {"CIUDADANO"})
    @DisplayName("I06: Declaración de comprobante duplicado retorna error 400 Bad Request")
    void declararPago_duplicado_retorna400() throws Exception {
        PagoExpediente previo = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .codigoComprobante("VCH-DUPLICADO-TEST")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .estado(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .fechaRegistro(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        pagoRepository.save(previo);

        DeclararPagoDto dto = DeclararPagoDto.builder()
                .codigoComprobante("VCH-DUPLICADO-TEST")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/pagos/declarar", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ya se encuentra registrado")));
    }

    @Test
    @WithMockUser(username = "cajero_pedro", roles = {"CAJERO"})
    @DisplayName("I06: Observación y rechazo de pago registran trazabilidad")
    void observarYRechazarPago_cajero_exito() throws Exception {
        PagoExpediente pago = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .expedienteId(expediente.getId())
                .codigoComprobante("VCH-SAT-2026-0003")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .estado(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .fechaRegistro(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        pago = pagoRepository.save(pago);

        ObservarPagoDto obsDto = ObservarPagoDto.builder()
                .motivo("Voucher borroso e ilegible en la fecha de emisión")
                .build();

        mockMvc.perform(post("/api/pagos/{id}/observar", pago.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(obsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OBSERVADO"))
                .andExpect(jsonPath("$.motivoObservacionORechazo").value("Voucher borroso e ilegible en la fecha de emisión"));

        RechazarPagoDto rechDto = RechazarPagoDto.builder()
                .motivo("Comprobante pertenece a trámite de arbitrios y no a licencias")
                .build();

        mockMvc.perform(post("/api/pagos/{id}/rechazar", pago.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rechDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));
    }

    @Test
    @WithMockUser(username = "evaluador_muni", roles = {"EVALUADOR"})
    @DisplayName("I06: Evaluador municipal registra compatibilidad de zonificación PDU (201 Created)")
    void registrarZonificacion_evaluador_retorna201() throws Exception {
        RegistrarEvaluacionZonificacionDto dto = RegistrarEvaluacionZonificacionDto.builder()
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .referenciaZonificacion("PDU Huamanga 2016-2026 / Zona Monumental (ZM)")
                .fuenteSustentoTecnico("Informe Técnico N° 102-2026-MPH-GDUR")
                .observaciones("Librería y venta de útiles es actividad compatible con la reglamentación del Centro Histórico.")
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/zonificacion/evaluar", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("COMPATIBLE"))
                .andExpect(jsonPath("$.evaluadorNombre").value("evaluador_muni"))
                .andExpect(jsonPath("$.fuenteSustentoTecnico").value("Informe Técnico N° 102-2026-MPH-GDUR"));
    }

    @Test
    @WithMockUser(username = CIUDADANO_DNI, roles = {"CIUDADANO"})
    @DisplayName("I06 Seguridad: Ciudadano no puede registrar evaluación de zonificación (403 Forbidden)")
    void registrarZonificacion_ciudadano_retorna403() throws Exception {
        RegistrarEvaluacionZonificacionDto dto = RegistrarEvaluacionZonificacionDto.builder()
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .referenciaZonificacion("PDU Huamanga")
                .fuenteSustentoTecnico("Dictamen no oficial")
                .observaciones("Intento de auto-evaluación")
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/zonificacion/evaluar", expediente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "99887766", roles = {"CIUDADANO"})
    @DisplayName("I06 BOLA/IDOR: Ciudadano ajeno no puede consultar pagos de otro expediente (403 Forbidden)")
    void consultarPagos_expedienteAjeno_retorna403() throws Exception {
        mockMvc.perform(get("/api/expedientes/{id}/pagos", expediente.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cajero_pedro", roles = {"CAJERO"})
    @DisplayName("I06: Compatibilidad con expedientes V1 sin romper datos históricos")
    void pagoExpedienteV1_compatibilidadHistorica() throws Exception {
        Expediente expV1 = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2023-000888")
                .solicitanteId(UUID.randomUUID())
                .cuentaId(null)
                .documentoIdentidad("41998877")
                .nombreTitular("Ferretería Central V1")
                .nombreComercial("Ferretería Central")
                .giroNegocio("Ferretería en general")
                .direccionEstablecimiento("Av. Mariscal Cáceres 780")
                .estado(EstadoExpediente.EN_EVALUACION)
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .montoTasa(new BigDecimal("218.00"))
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(21))
                .build();
        expV1 = expedienteRepository.save(expV1);

        DeclararPagoDto dto = DeclararPagoDto.builder()
                .codigoComprobante("VCH-V1-HISTORICO-01")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("218.00"))
                .observacionesAdministrado("Caja presencial ventanilla municipal")
                .build();

        mockMvc.perform(post("/api/expedientes/{id}/pagos/declarar", expV1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoComprobante").value("VCH-V1-HISTORICO-01"));
    }
}
