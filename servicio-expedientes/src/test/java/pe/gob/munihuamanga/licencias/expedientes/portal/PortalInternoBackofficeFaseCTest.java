package pe.gob.munihuamanga.licencias.expedientes.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.gob.munihuamanga.licencias.common.dto.DeclararPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarPagoManualDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración automatizadas para la Fase C.2:
 * - RBAC sobre Bandeja de Pagos SAT (CAJERO y ADMIN autorizados; CIUDADANO denegado 403)
 * - Módulo SAT: Calificación manual de pagos, justificación obligatoria e inmutabilidad
 * - Independencia de estados: La validación de caja no muta prematuramente a APROBADO
 * - Expedientes en estado PRESENTADO accesibles en la bandeja interna V2
 * - Consulta de Cuentas y Operadores con control de acceso (M01)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PortalInternoBackofficeFaseCTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Test
    @DisplayName("Bandeja de pagos: cajero y auditor autorizados, administrado denegado")
    void testRbacBandejaPagosSat() throws Exception {
        String tokenCajero = obtenerToken("cajero", "Caja.Huamanga2026");
        String tokenAuditor = obtenerToken("auditor", "Control.Huamanga26");
        String tokenCiudadano = obtenerToken("42567891", "Ciudadano.Huamanga26");

        // 1. Cajero accede a la bandeja SAT
        mockMvc.perform(get("/api/pagos/bandeja")
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isOk());

        // 2. El auditor consulta la bandeja SAT (solo lectura)
        mockMvc.perform(get("/api/pagos/bandeja")
                        .header("Authorization", "Bearer " + tokenAuditor))
                .andExpect(status().isOk());

        // 3. Ciudadano recibe 403 Forbidden
        mockMvc.perform(get("/api/pagos/bandeja")
                        .header("Authorization", "Bearer " + tokenCiudadano))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Fase C.2 - 02: Validación Manual de Pago SAT por Cajero con Trazabilidad e Independencia de Estados")
    void testValidacionManualPagoSatPorCajero() throws Exception {
        String tokenCiudadano = obtenerToken("42567891", "Ciudadano.Huamanga26");
        String tokenCajero = obtenerToken("cajero", "Caja.Huamanga2026");

        // 1. Crear expediente de prueba en estado PRESENTADO
        Expediente exp = crearExpedientePrueba("EXP-2026-C2001", EstadoExpediente.PENDIENTE_PAGO, new BigDecimal("154.50"));

        // 2. Ciudadano declara comprobante de pago
        String codigoVoucher = "VCH-SAT-" + System.currentTimeMillis();
        DeclararPagoDto decDto = DeclararPagoDto.builder()
                .codigoComprobante(codigoVoucher)
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("154.50"))
                .observacionesAdministrado("Pago realizado en ventanilla SAT Huamanga")
                .build();

        MvcResult resDec = mockMvc.perform(post("/api/expedientes/" + exp.getId() + "/pagos/declarar")
                        .header("Authorization", "Bearer " + tokenCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_VALIDACION"))
                .andReturn();

        UUID pagoId = UUID.fromString(objectMapper.readTree(resDec.getResponse().getContentAsString()).get("id").asText());

        // 3. Cajero SAT valida manualmente el comprobante
        ValidarPagoManualDto valDto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-SAT-2026-8800")
                .montoValidado(new BigDecimal("154.50"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Cotejado físicamente con el arqueo de caja SAT ventanilla 1")
                .build();

        mockMvc.perform(post("/api/pagos/" + pagoId + "/validar")
                        .header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDADO_MANUALMENTE"))
                .andExpect(jsonPath("$.validadorNombre").value("cajero"))
                .andExpect(jsonPath("$.numeroOperacionSat").value("OP-SAT-2026-8800"));

        // 4. Consultar historial de decisiones de auditoría (M05)
        mockMvc.perform(get("/api/pagos/" + pagoId + "/historial")
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].estadoNuevo").value("VALIDADO_MANUALMENTE"));

        // 5. Con el pago validado el expediente pasa a evaluación; nunca a un estado de fondo
        Expediente expActualizado = expedienteRepository.findById(exp.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(EstadoExpediente.EN_EVALUACION, expActualizado.getEstado(),
                "La validación del pago completa el expediente y lo pasa a evaluación");
        org.junit.jupiter.api.Assertions.assertEquals(codigoVoucher, expActualizado.getVoucherId());
    }

    @Test
    @DisplayName("Fase C.2 - 03: Validación SAT: Rechazo por justificación corta o importe insuficiente")
    void testReglasValidacionSatCajero() throws Exception {
        String tokenCiudadano = obtenerToken("42567891", "Ciudadano.Huamanga26");
        String tokenCajero = obtenerToken("cajero", "Caja.Huamanga2026");

        Expediente exp = crearExpedientePrueba("EXP-2026-C2002", EstadoExpediente.PENDIENTE_PAGO, new BigDecimal("218.00"));

        String codigoVoucher = "VCH-SAT-" + (System.currentTimeMillis() + 10);
        DeclararPagoDto decDto = DeclararPagoDto.builder()
                .codigoComprobante(codigoVoucher)
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("218.00"))
                .build();

        MvcResult resDec = mockMvc.perform(post("/api/expedientes/" + exp.getId() + "/pagos/declarar")
                        .header("Authorization", "Bearer " + tokenCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decDto)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID pagoId = UUID.fromString(objectMapper.readTree(resDec.getResponse().getContentAsString()).get("id").asText());

        // A. Falla por justificación menor a 10 caracteres
        ValidarPagoManualDto valDtoCorta = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-123")
                .montoValidado(new BigDecimal("218.00"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Ok") // Demasiado corta
                .build();

        mockMvc.perform(post("/api/pagos/" + pagoId + "/validar")
                        .header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valDtoCorta)))
                .andExpect(status().isBadRequest());

        // B. Falla por monto menor a la tasa TUPA oficial (S/. 100 vs S/. 218)
        ValidarPagoManualDto valDtoInsuficiente = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-123")
                .montoValidado(new BigDecimal("100.00"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Monto incompleto entregado en caja")
                .build();

        mockMvc.perform(post("/api/pagos/" + pagoId + "/validar")
                        .header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valDtoInsuficiente)))
                .andExpect(status().isBadRequest());

        // C. Observar pago formalmente
        ObservarPagoDto obsDto = new ObservarPagoDto("Comprobante ilegible en el sello de caja");
        mockMvc.perform(post("/api/pagos/" + pagoId + "/observar")
                        .header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(obsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OBSERVADO"));
    }

    @Test
    @DisplayName("Fase C.2 - 04: Bandeja de Expedientes V2: Consulta y filtrado por estado PRESENTADO")
    void testConsultaExpedientesPresentados() throws Exception {
        String tokenEvaluador = obtenerToken("evaluador", "Licencias.Huamanga26");

        // Asegurar que existe al menos un expediente presentado
        crearExpedientePrueba("EXP-2026-C2003", EstadoExpediente.PRESENTADO, new BigDecimal("154.50"));

        mockMvc.perform(get("/api/expedientes?estado=PRESENTADO")
                        .header("Authorization", "Bearer " + tokenEvaluador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].estado").value("PRESENTADO"));
    }

    @Test
    @DisplayName("Fase C.2 - 05: Consulta de Cuentas y Operadores (M01): Autorizado para evaluador/admin")
    void testConsultaCuentasOperadoresM01() throws Exception {
        String tokenEvaluador = obtenerToken("evaluador", "Licencias.Huamanga26");

        // Consultar cuenta semilla de María Quispe (42567891)
        mockMvc.perform(get("/api/cuentas/documento/42567891")
                        .header("Authorization", "Bearer " + tokenEvaluador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDocumento").value("42567891"))
                .andExpect(jsonPath("$.razonSocialONombre").value("María Quispe Huamán"));
    }

    private Expediente crearExpedientePrueba(String numTramite, EstadoExpediente estado, BigDecimal tasa) {
        LocalDateTime ahora = LocalDateTime.now();
        UUID cuentaId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
        Expediente exp = Expediente.builder()
                .id(UUID.randomUUID())
                .solicitanteId(cuentaId)
                .cuentaId(cuentaId)
                .numeroTramite(numTramite)
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad("42567891")
                .correoElectronico("maria.quispe@gmail.com")
                .telefono("966123456")
                .nombreComercial("Botica San Juan Bautista - Sucursal Centro")
                .giroNegocio("Venta minorista de productos farmacéuticos")
                .direccionEstablecimiento("Jr. 28 de Julio 450, Ayacucho")
                .distrito("Ayacucho")
                .provincia("Huamanga")
                .departamento("Ayacucho")
                .areaMetrosCuadrados(new BigDecimal("75.00"))
                .aforoPersonas(25)
                .numeroPisos(1)
                .estado(estado)
                .nivelRiesgo(NivelRiesgo.BAJO)
                .montoTasa(tasa)
                .fechaCreacion(ahora)
                .fechaLimite(ahora.plusDays(15))
                .build();
        return expedienteRepository.save(exp);
    }

    private String obtenerToken(String username, String password) throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto(username, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }
}
