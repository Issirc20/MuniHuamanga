package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.dto.CrearExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.exception.TransicionInvalidaException;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.validator.EstadoExpedienteValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpedienteServiceTest {

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Spy
    private EstadoExpedienteValidator estadoExpedienteValidator = new EstadoExpedienteValidator();

    @Spy
    private CalculadoraDeTasa calculadoraDeTasa = new CalculadoraDeTasa(
            new BigDecimal("154.50"),
            new BigDecimal("218.00"),
            new BigDecimal("345.20"),
            new BigDecimal("480.00")
    );

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private MetricasExpedienteService metricasExpedienteService;

    @Mock
    private NotificacionEmailService notificacionEmailService;  // Fase 04 Sprint 4-B

    @InjectMocks
    private ExpedienteService expedienteService;


    private UUID expedienteId;
    private Expediente expedienteBase;

    @BeforeEach
    void setUp() {
        expedienteId = UUID.randomUUID();
        expedienteBase = Expediente.builder()
                .id(expedienteId)
                .numeroTramite("EXP-2026-12345")
                .solicitanteId(UUID.randomUUID())
                .nombreTitular("Juan Perez Mendoza")
                .documentoIdentidad("45879632")
                .nombreComercial("Bodega El Huamanguino")
                .giroNegocio("Venta de abarrotes al por menor")
                .direccionEstablecimiento("Jr. 28 de Julio 345, Ayacucho")
                .areaMetrosCuadrados(new BigDecimal("45.00"))
                .correoElectronico("juan.perez@gmail.com")
                .telefono("966987654")
                .estado(EstadoExpediente.FORMATOS_GENERADOS)
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(21))
                .build();
    }

    @Test
    @DisplayName("Debe crear expediente correctamente con estado FORMATOS_GENERADOS y auditoría")
    void testCrearExpediente() {
        CrearExpedienteDto dto = CrearExpedienteDto.builder()
                .solicitanteId(UUID.randomUUID())
                .nombreTitular("Juan Perez Mendoza")
                .documentoIdentidad("45879632")
                .nombreComercial("Bodega El Huamanguino")
                .giroNegocio("Venta de abarrotes")
                .direccionEstablecimiento("Jr. 28 de Julio 345")
                .areaMetrosCuadrados(new BigDecimal("45.00"))
                .correoElectronico("juan.perez@gmail.com")
                .telefono("966987654")
                .build();

        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expediente creado = expedienteService.crearExpediente(dto);

        assertNotNull(creado);
        assertEquals(EstadoExpediente.FORMATOS_GENERADOS, creado.getEstado());
        assertNotNull(creado.getNumeroTramite());
        verify(expedienteRepository).save(any(Expediente.class));
        verify(auditoriaService).registrarTransicion(eq(creado.getId()), eq(null), eq(EstadoExpediente.FORMATOS_GENERADOS), any(), any());
    }

    @Test
    @DisplayName("Debe registrar clasificación de riesgo ITSE y cambiar estado a DOCUMENTOS_VALIDADOS")
    void testRegistrarClasificacionRiesgo() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expedienteService.registrarClasificacionRiesgo(expedienteId, NivelRiesgo.BAJO);

        assertEquals(EstadoExpediente.DOCUMENTOS_VALIDADOS, expedienteBase.getEstado());
        assertEquals(NivelRiesgo.BAJO, expedienteBase.getNivelRiesgo());
        assertEquals(new BigDecimal("154.50"), expedienteBase.getMontoTasa());
        verify(auditoriaService).registrarTransicion(eq(expedienteId), eq(EstadoExpediente.FORMATOS_GENERADOS), eq(EstadoExpediente.DOCUMENTOS_VALIDADOS), any(), any());
    }

    @Test
    @DisplayName("Debe generar voucher SAT con monto de tasa asignado")
    void testGenerarVoucher() {
        expedienteBase.setMontoTasa(new BigDecimal("154.50"));
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoucherDto voucher = expedienteService.generarVoucher(expedienteId);

        assertNotNull(voucher);
        assertNotNull(voucher.getVoucherId());
        assertEquals(new BigDecimal("154.50"), voucher.getMonto());
        assertNotNull(expedienteBase.getVoucherId());
    }

    @Test
    @DisplayName("Debe registrar pago del voucher y pasar a EN_EVALUACION_FINAL")
    void testRegistrarPago() {
        expedienteBase.setEstado(EstadoExpediente.DOCUMENTOS_VALIDADOS);
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expedienteService.registrarPago(expedienteId, "VCH-2026-999888");

        assertEquals(EstadoExpediente.EN_EVALUACION_FINAL, expedienteBase.getEstado());
        verify(auditoriaService).registrarTransicion(eq(expedienteId), eq(EstadoExpediente.DOCUMENTOS_VALIDADOS), eq(EstadoExpediente.EN_EVALUACION_FINAL), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intenta aprobar un expediente sin voucher pagado")
    void testBloquearAprobacionSinPago() {
        expedienteBase.setEstado(EstadoExpediente.EN_EVALUACION_FINAL);
        expedienteBase.setNivelRiesgo(NivelRiesgo.BAJO);
        expedienteBase.setVoucherId(null); // No ha pagado

        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));

        assertThrows(TransicionInvalidaException.class, () -> expedienteService.aprobar(expedienteId));
    }

    @Test
    @DisplayName("Debe aprobar expediente cuando cumple con ITSE y pago, generando código QR único")
    void testAprobarExpedienteConforme() {
        expedienteBase.setEstado(EstadoExpediente.EN_EVALUACION_FINAL);
        expedienteBase.setNivelRiesgo(NivelRiesgo.MEDIO);
        expedienteBase.setVoucherId("VCH-2026-123456"); // Pagado
        expedienteBase.setMontoTasa(new BigDecimal("218.00"));

        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expedienteService.aprobar(expedienteId);

        assertEquals(EstadoExpediente.APROBADO, expedienteBase.getEstado());
        assertNotNull(expedienteBase.getLicenciaQrCode());
        verify(auditoriaService).registrarTransicion(eq(expedienteId), eq(EstadoExpediente.EN_EVALUACION_FINAL), eq(EstadoExpediente.APROBADO), any(), any());
    }

    @Test
    @DisplayName("Debe crear expediente completo con datos de Anexo 1 y Anexo 4")
    void testCrearExpedienteConAnexo1Y4() {
        CrearExpedienteDto dto = CrearExpedienteDto.builder()
                .solicitanteId(UUID.randomUUID())
                .tipoPersona(pe.gob.munihuamanga.licencias.common.enums.TipoPersona.JURIDICA)
                .tipoDocumento(pe.gob.munihuamanga.licencias.common.enums.TipoDocumento.RUC)
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad("20601234567")
                .razonSocial("INVERSIONES LOS RETABLOS S.A.C.")
                .partidaSunarp("11029384")
                .asientoSunarp("A0001")
                .dniRepresentante("42567891")
                .nombreRepresentante("María Quispe Huamán")
                .modalidadTramite(pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite.LICENCIA_INDETERMINADA)
                .nombreComercial("Boutique Artesanal Huamanga")
                .giroNegocio("Venta de artesanías")
                .ciiuCodigo("4773")
                .direccionEstablecimiento("Jr. 9 de Diciembre 142")
                .tipoVia("Jr.")
                .nombreVia("9 de Diciembre")
                .numeroVivienda("142")
                .urbanizacion("Centro Histórico")
                .areaMetrosCuadrados(new BigDecimal("35.50"))
                .aforoPersonas(15)
                .funcionEdificacion(pe.gob.munihuamanga.licencias.common.enums.FuncionEdificacion.COMERCIO)
                .correoElectronico("contacto@losretablos.pe")
                .telefono("966123456")
                .anexo4Condiciones(pe.gob.munihuamanga.licencias.common.dto.Anexo4CondicionesDto.builder()
                        .aforoPersonas(15)
                        .areaTerreno(new BigDecimal("40.00"))
                        .extintoresOperativos(true)
                        .pozoTierraVigente(true)
                        .build())
                .build();

        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expediente creado = expedienteService.crearExpediente(dto);

        assertNotNull(creado);
        assertEquals(pe.gob.munihuamanga.licencias.common.enums.TipoPersona.JURIDICA, creado.getTipoPersona());
        assertEquals("20601234567", creado.getDocumentoIdentidad());
        assertEquals("11029384", creado.getPartidaSunarp());
        assertEquals(pe.gob.munihuamanga.licencias.common.enums.FuncionEdificacion.COMERCIO, creado.getFuncionEdificacion());
        assertNotNull(creado.getAnexo4Condiciones());
        assertEquals(15, creado.getAnexo4Condiciones().getAforoPersonas());
    }

    @Test
    @DisplayName("Debe registrar clasificación ITSE con número de informe y observaciones")
    void testRegistrarClasificacionItseConInforme() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));

        expedienteService.registrarClasificacionRiesgo(expedienteId, NivelRiesgo.MEDIO, "INF-ITSE-2026-0045", "Conforme");

        assertEquals(EstadoExpediente.DOCUMENTOS_VALIDADOS, expedienteBase.getEstado());
        assertEquals(NivelRiesgo.MEDIO, expedienteBase.getNivelRiesgo());
        assertEquals("INF-ITSE-2026-0045", expedienteBase.getNumeroInformeItse());
        assertNotNull(expedienteBase.getFechaInformeItse());
    }

    @Test
    @DisplayName("Debe registrar pago de tasa con número de operación SAT")
    void testRegistrarPagoConOperacionSat() {
        expedienteBase.setEstado(EstadoExpediente.DOCUMENTOS_VALIDADOS);
        expedienteBase.setNivelRiesgo(NivelRiesgo.BAJO);
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));

        expedienteService.registrarPago(expedienteId, "VCH-2026-000123", "OP-SAT-998877");

        assertEquals(EstadoExpediente.EN_EVALUACION_FINAL, expedienteBase.getEstado());
        assertEquals("VCH-2026-000123", expedienteBase.getVoucherId());
        assertEquals("OP-SAT-998877", expedienteBase.getNumeroOperacionSat());
        assertNotNull(expedienteBase.getFechaPagoSat());
    }

    @Test
    @DisplayName("Fase 3: Debe observar expediente registrando motivo, fecha y auditoría")
    void testObservarExpediente() {
        expedienteBase.setEstado(EstadoExpediente.DOCUMENTOS_VALIDADOS);
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));

        expedienteService.observar(expedienteId, "Falta adjuntar plano de distribución actualizado");

        assertEquals(EstadoExpediente.OBSERVADO, expedienteBase.getEstado());
        assertEquals("Falta adjuntar plano de distribución actualizado", expedienteBase.getMotivoObservacion());
        assertNotNull(expedienteBase.getFechaObservacion());
        verify(auditoriaService).registrarTransicion(eq(expedienteId), eq(EstadoExpediente.DOCUMENTOS_VALIDADOS), eq(EstadoExpediente.OBSERVADO), any(), any());
        verify(notificacionEmailService).notificarObservacion(eq(expedienteBase), eq("Falta adjuntar plano de distribución actualizado"));
    }

    @Test
    @DisplayName("Fase 3: Debe subsanar expediente observado retornando al flujo")
    void testSubsanarExpediente() {
        expedienteBase.setEstado(EstadoExpediente.OBSERVADO);
        expedienteBase.setNivelRiesgo(NivelRiesgo.MEDIO);
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));

        expedienteService.subsanar(expedienteId, "Se adjunta plano sellado por arquitecto colegiado", "Juan Pérez");

        assertEquals(EstadoExpediente.DOCUMENTOS_VALIDADOS, expedienteBase.getEstado());
        assertEquals("Se adjunta plano sellado por arquitecto colegiado", expedienteBase.getDetalleSubsanacion());
        assertNotNull(expedienteBase.getFechaSubsanacion());
        verify(auditoriaService).registrarTransicion(eq(expedienteId), eq(EstadoExpediente.OBSERVADO), eq(EstadoExpediente.DOCUMENTOS_VALIDADOS), eq("Juan Pérez"), any());
    }

    @Test
    @DisplayName("Fase 3: Debe fallar al intentar subsanar un expediente que no está en estado OBSERVADO")
    void testSubsanarExpedienteNoObservadoLanzaExcepcion() {
        expedienteBase.setEstado(EstadoExpediente.FORMATOS_GENERADOS);
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteBase));

        assertThrows(IllegalStateException.class, () ->
                expedienteService.subsanar(expedienteId, "Subsanación prematura", "Juan Pérez")
        );
    }
}
