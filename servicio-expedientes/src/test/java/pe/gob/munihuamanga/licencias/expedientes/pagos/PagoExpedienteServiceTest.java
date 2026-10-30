package pe.gob.munihuamanga.licencias.expedientes.pagos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.gob.munihuamanga.licencias.common.dto.DeclararPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialDecisionPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.PagoExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.RechazarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarPagoManualDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ComandosExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.security.ServicioRepresentacion;
import pe.gob.munihuamanga.licencias.expedientes.security.VerificadorTitularidad;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.HistorialDecisionPago;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.PagoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.HistorialDecisionPagoRepository;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.PagoExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.pagos.service.PagoExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoExpedienteServiceTest {

    @Mock
    private PagoExpedienteRepository pagoRepository;

    @Mock
    private HistorialDecisionPagoRepository historialRepository;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private ComandosExpediente comandosExpediente;

    @Mock
    private ProveedorActor proveedorActor;

    @Mock
    private ServicioRepresentacion servicioRepresentacion;

    @Mock
    private AuditoriaService auditoriaService;

    private PagoExpedienteService service;

    private UUID expedienteId;
    private UUID titularId;
    private Expediente expediente;

    @BeforeEach
    void setUp() {
        service = new PagoExpedienteService(pagoRepository, historialRepository, expedienteRepository, auditoriaService,
                comandosExpediente, new VerificadorTitularidad(proveedorActor, servicioRepresentacion));

        expedienteId = UUID.randomUUID();
        titularId = UUID.randomUUID();
        lenient().when(proveedorActor.actual()).thenReturn(new Actor(titularId, "42567891", "Titular de prueba", java.util.EnumSet.of(RolUsuario.ROLE_CIUDADANO)));
        lenient().when(servicioRepresentacion.tieneFacultad(any(), any(), any(PermisoOperador[].class))).thenReturn(true);

        expediente = Expediente.builder()
                .id(expedienteId)
                .numeroTramite("EXP-2026-00450")
                .solicitanteId(titularId)
                .cuentaId(titularId)
                .documentoIdentidad("42567891")
                .estado(EstadoExpediente.PENDIENTE_PAGO)
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .montoTasa(new BigDecimal("218.00"))
                .build();
    }

    @Test
    @DisplayName("Declarar pago: voucher registrado queda en PENDIENTE_VALIDACION sin auto-aprobación")
    void declararPago_voucherPendiente_exito() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(pagoRepository.existsByCodigoComprobante("VCH-2026-9901")).thenReturn(false);
        when(pagoRepository.save(any(PagoExpediente.class))).thenAnswer(i -> i.getArgument(0));

        DeclararPagoDto dto = DeclararPagoDto.builder()
                .codigoComprobante("vch-2026-9901")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("218.00"))
                .observacionesAdministrado("Pago en agencia SAT Jr. Arequipa")
                .build();

        PagoExpedienteDto res = service.declararPago(expedienteId, dto);

        assertThat(res).isNotNull();
        assertThat(res.getCodigoComprobante()).isEqualTo("VCH-2026-9901");
        assertThat(res.getEstado()).isEqualTo(EstadoValidacionPago.PENDIENTE_VALIDACION);
        assertThat(res.getTasaTupaEsperada()).isEqualByComparingTo("218.00");
        verify(historialRepository).save(any(HistorialDecisionPago.class));
    }

    @Test
    @DisplayName("Declarar pago: comprobante duplicado es rechazado con excepción")
    void declararPago_comprobanteDuplicado_lanzaExcepcion() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(pagoRepository.existsByCodigoComprobante("VCH-DUPLICADO")).thenReturn(true);

        DeclararPagoDto dto = DeclararPagoDto.builder()
                .codigoComprobante("VCH-DUPLICADO")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("218.00"))
                .build();

        assertThatThrownBy(() -> service.declararPago(expedienteId, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra registrado");
    }

    @Test
    @DisplayName("Validación manual autorizada: cajero confirma pago y previene auto-aprobación del expediente")
    void validarPagoManual_cajeroAutorizado_previeneAprobacionAutomatica() {
        UUID pagoId = UUID.randomUUID();
        PagoExpediente pago = PagoExpediente.builder()
                .id(pagoId)
                .expedienteId(expedienteId)
                .codigoComprobante("VCH-2026-5511")
                .fechaDeclarada(LocalDate.now().minusDays(1))
                .montoDeclarado(new BigDecimal("218.00"))
                .estado(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .tasaTupaEsperada(new BigDecimal("218.00"))
                .fechaRegistro(LocalDateTime.now().minusDays(1))
                .fechaActualizacion(LocalDateTime.now().minusDays(1))
                .build();

        when(pagoRepository.findByIdWithLock(pagoId)).thenReturn(Optional.of(pago));
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(pagoRepository.save(any(PagoExpediente.class))).thenAnswer(i -> i.getArgument(0));

        ValidarPagoManualDto dto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-SAT-998822")
                .montoValidado(new BigDecimal("218.00"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Comprobante cotejado con reporte de caja SAT ventanilla 3")
                .build();

        UUID cajeroId = UUID.randomUUID();
        PagoExpedienteDto res = service.validarPagoManual(pagoId, dto, cajeroId, "cajero_muni");

        assertThat(res.getEstado()).isEqualTo(EstadoValidacionPago.VALIDADO_MANUALMENTE);
        assertThat(res.getNumeroOperacionSat()).isEqualTo("OP-SAT-998822");
        assertThat(res.getValidadorNombre()).isEqualTo("cajero_muni");

        // Regla I06.1: Independencia de estados — el expediente NO muta a DOCUMENTOS_VALIDADOS ni APROBADO
        verify(comandosExpediente).registrar(eq(expedienteId), eq(EventoExpediente.PAGO_VALIDADO), any());
        assertThat(expediente.getVoucherId()).isEqualTo("VCH-2026-5511");
        assertThat(expediente.getNumeroOperacionSat()).isEqualTo("OP-SAT-998822");
        verify(historialRepository).save(any(HistorialDecisionPago.class));
    }

    @Test
    @DisplayName("Doble validación: intento de revalidar comprobante ya validado lanza excepción")
    void validarPagoManual_pagoYaValidado_lanzaIllegalStateException() {
        UUID pagoId = UUID.randomUUID();
        PagoExpediente pago = PagoExpediente.builder()
                .id(pagoId)
                .expedienteId(expedienteId)
                .codigoComprobante("VCH-2026-YA-VALIDADO")
                .estado(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .build();

        when(pagoRepository.findByIdWithLock(pagoId)).thenReturn(Optional.of(pago));

        ValidarPagoManualDto dto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-SAT-RETRY")
                .montoValidado(new BigDecimal("218.00"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Reintento simultáneo o secundario de validación")
                .build();

        assertThatThrownBy(() -> service.validarPagoManual(pagoId, dto, UUID.randomUUID(), "cajero_dos"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue validado manualmente con anterioridad");
    }

    @Test
    @DisplayName("Integridad: intento de validar un pago previamente RECHAZADO lanza excepción")
    void validarPagoManual_pagoRechazado_lanzaIllegalStateException() {
        UUID pagoId = UUID.randomUUID();
        PagoExpediente pago = PagoExpediente.builder()
                .id(pagoId)
                .expedienteId(expedienteId)
                .codigoComprobante("VCH-RECHAZADO-01")
                .estado(EstadoValidacionPago.RECHAZADO)
                .build();

        when(pagoRepository.findByIdWithLock(pagoId)).thenReturn(Optional.of(pago));

        ValidarPagoManualDto dto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-SAT-FAIL")
                .montoValidado(new BigDecimal("218.00"))
                .fechaPagoReal(LocalDate.now())
                .justificacion("Intento indebido de validar un voucher nulo")
                .build();

        assertThatThrownBy(() -> service.validarPagoManual(pagoId, dto, UUID.randomUUID(), "cajero_muni"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ha sido RECHAZADO");
    }

    @Test
    @DisplayName("Normalización de comprobante: espacios redundantes se normalizan y detectan duplicidad")
    void declararPago_codigoNormalizado_detectaDuplicidad() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(pagoRepository.existsByCodigoComprobante("VCH-2026-9901")).thenReturn(true);

        DeclararPagoDto dto = DeclararPagoDto.builder()
                .codigoComprobante("  vch - 2026 - 9901  ")
                .fechaDeclarada(LocalDate.now())
                .montoDeclarado(new BigDecimal("218.00"))
                .build();

        assertThatThrownBy(() -> service.declararPago(expedienteId, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra registrado");
    }

    @Test
    @DisplayName("Tasa TUPA verificada: monto validado inferior a tasa institucional es rechazado")
    void validarPagoManual_montoInferiorATasaTupa_lanzaExcepcion() {
        UUID pagoId = UUID.randomUUID();
        PagoExpediente pago = PagoExpediente.builder()
                .id(pagoId)
                .expedienteId(expedienteId)
                .codigoComprobante("VCH-INCOMPLETO")
                .estado(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .tasaTupaEsperada(new BigDecimal("218.00"))
                .build();

        when(pagoRepository.findByIdWithLock(pagoId)).thenReturn(Optional.of(pago));

        ValidarPagoManualDto dto = ValidarPagoManualDto.builder()
                .numeroOperacionSat("OP-SAT-LOW")
                .montoValidado(new BigDecimal("50.00")) // Menor que 218.00
                .fechaPagoReal(LocalDate.now())
                .justificacion("Pago parcial no admitido según TUPA")
                .build();

        assertThatThrownBy(() -> service.validarPagoManual(pagoId, dto, UUID.randomUUID(), "cajero_muni"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("es inferior a la tasa TUPA oficial exigida");
    }

    @Test
    @DisplayName("Observación y rechazo de pago: registran motivación y decisión en historial")
    void observarYRechazarPago_registranHistorial() {
        UUID pagoId = UUID.randomUUID();
        PagoExpediente pago = PagoExpediente.builder()
                .id(pagoId)
                .expedienteId(expedienteId)
                .codigoComprobante("VCH-2026-8800")
                .estado(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .build();

        when(pagoRepository.findByIdWithLock(pagoId)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(PagoExpediente.class))).thenAnswer(i -> i.getArgument(0));

        ObservarPagoDto obsDto = ObservarPagoDto.builder()
                .motivo("Monto consignado no coincide con la tasa TUPA de riesgo MEDIO")
                .build();

        UUID cajeroId = UUID.randomUUID();
        PagoExpedienteDto resObs = service.observarPago(pagoId, obsDto, cajeroId, "cajero_muni");
        assertThat(resObs.getEstado()).isEqualTo(EstadoValidacionPago.OBSERVADO);

        RechazarPagoDto rechDto = RechazarPagoDto.builder()
                .motivo("Voucher anulado en banco emisor")
                .build();

        PagoExpedienteDto resRech = service.rechazarPago(pagoId, rechDto, cajeroId, "cajero_muni");
        assertThat(resRech.getEstado()).isEqualTo(EstadoValidacionPago.RECHAZADO);
    }

    @Test
    @DisplayName("Control BOLA/IDOR: deniega acceso a pagos de expediente ajeno")
    void consultarPagos_expedienteAjeno_deniegaAcceso() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(servicioRepresentacion.tieneFacultad(any(), any(), any(PermisoOperador[].class))).thenReturn(false);

        assertThatThrownBy(() -> service.listarPagosPorExpediente(expedienteId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No cuenta con autorización");
    }

    @Test
    @DisplayName("Auditoría de decisiones: recupera trazabilidad inmutable de un pago")
    void obtenerHistorialPago_retornaAuditoriaCompleta() {
        UUID pagoId = UUID.randomUUID();
        PagoExpediente pago = PagoExpediente.builder()
                .id(pagoId)
                .expedienteId(expedienteId)
                .build();
        when(pagoRepository.findById(pagoId)).thenReturn(Optional.of(pago));
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));

        HistorialDecisionPago h1 = HistorialDecisionPago.builder()
                .id(UUID.randomUUID())
                .pagoId(pagoId)
                .estadoAnterior(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .estadoNuevo(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .usuarioNombre("cajero_juan")
                .motivo("Validado conforme a TUPA")
                .fechaDecision(LocalDateTime.now())
                .build();
        when(historialRepository.findByPagoIdOrderByFechaDecisionDesc(pagoId)).thenReturn(List.of(h1));

        List<HistorialDecisionPagoDto> lista = service.obtenerHistorialPago(pagoId);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getEstadoNuevo()).isEqualTo(EstadoValidacionPago.VALIDADO_MANUALMENTE);
    }
}
