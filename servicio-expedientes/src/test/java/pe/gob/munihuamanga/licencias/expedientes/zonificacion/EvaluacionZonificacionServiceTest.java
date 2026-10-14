package pe.gob.munihuamanga.licencias.expedientes.zonificacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.gob.munihuamanga.licencias.common.dto.EvaluacionZonificacionDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistrarEvaluacionZonificacionDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.model.EvaluacionZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.repository.EvaluacionZonificacionRepository;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.service.EvaluacionZonificacionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluacionZonificacionServiceTest {

    @Mock
    private EvaluacionZonificacionRepository zonificacionRepository;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private EstablecimientoRepository establecimientoRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private OperadorCuentaRepository operadorCuentaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private EvaluacionZonificacionService service;

    private UUID expedienteId;
    private UUID titularId;
    private UUID establecimientoId;
    private Expediente expediente;
    private Establecimiento establecimiento;

    @BeforeEach
    void setUp() {
        service = new EvaluacionZonificacionService(
                zonificacionRepository,
                expedienteRepository,
                establecimientoRepository,
                cuentaRepository,
                operadorCuentaRepository,
                auditoriaService
        );

        expedienteId = UUID.randomUUID();
        titularId = UUID.randomUUID();
        establecimientoId = UUID.randomUUID();

        expediente = Expediente.builder()
                .id(expedienteId)
                .numeroTramite("EXP-2026-00890")
                .solicitanteId(titularId)
                .cuentaId(titularId)
                .establecimientoId(establecimientoId)
                .documentoIdentidad("42567891")
                .giroNegocio("Farmacia y Botica")
                .direccionEstablecimiento("Jr. 28 de Julio 450")
                .estado(EstadoExpediente.DOCUMENTOS_VALIDADOS)
                .build();

        establecimiento = Establecimiento.builder()
                .id(establecimientoId)
                .direccion("Jr. 28 de Julio 450")
                .giroPrincipal("Farmacia y Botica")
                .latitud(new BigDecimal("-13.1633000"))
                .longitud(new BigDecimal("-74.2236000"))
                .build();
    }

    @Test
    @DisplayName("Zonificación: evaluador autorizado registra compatibilidad técnica motivada")
    void registrarEvaluacion_funcionarioAutorizado_compatible() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.findById(establecimientoId)).thenReturn(Optional.of(establecimiento));
        when(zonificacionRepository.save(any(EvaluacionZonificacion.class))).thenAnswer(i -> i.getArgument(0));

        RegistrarEvaluacionZonificacionDto dto = RegistrarEvaluacionZonificacionDto.builder()
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .referenciaZonificacion("PDU Huamanga 2016-2026 / Zona Comercio Central (CC)")
                .fuenteSustentoTecnico("Informe Técnico N° 088-2026-MPH-GDUR")
                .observaciones("La actividad de farmacia resulta compatible y permitida en Zona CC según el índice de usos.")
                .build();

        UUID evaluadorId = UUID.randomUUID();
        EvaluacionZonificacionDto res = service.registrarEvaluacion(expedienteId, dto, evaluadorId, "arq_evaluador");

        assertThat(res).isNotNull();
        assertThat(res.getResultado()).isEqualTo(ResultadoZonificacion.COMPATIBLE);
        assertThat(res.getReferenciaZonificacion()).contains("PDU Huamanga");
        assertThat(res.getEvaluadorNombre()).isEqualTo("arq_evaluador");
        verify(auditoriaService).registrarTransicion(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Zonificación: actividad no compatible registra dictamen negativo sin emitir certificado automático")
    void registrarEvaluacion_noCompatible_registraDictamen() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.findById(establecimientoId)).thenReturn(Optional.of(establecimiento));
        when(zonificacionRepository.save(any(EvaluacionZonificacion.class))).thenAnswer(i -> i.getArgument(0));

        RegistrarEvaluacionZonificacionDto dto = RegistrarEvaluacionZonificacionDto.builder()
                .resultado(ResultadoZonificacion.NO_COMPATIBLE)
                .referenciaZonificacion("PDU Huamanga 2016-2026 / Zona Residencial de Densidad Baja (RDB)")
                .fuenteSustentoTecnico("Informe Técnico N° 089-2026-MPH-GDUR")
                .observaciones("Giro comercial no admisible en zona estrictamente residencial.")
                .build();

        UUID evaluadorId = UUID.randomUUID();
        EvaluacionZonificacionDto res = service.registrarEvaluacion(expedienteId, dto, evaluadorId, "arq_evaluador");

        assertThat(res.getResultado()).isEqualTo(ResultadoZonificacion.NO_COMPATIBLE);
        assertThat(res.getObservaciones()).contains("Giro comercial no admisible");
    }

    @Test
    @DisplayName("Zonificación: establecimiento inconsistente con el expediente es rechazado")
    void registrarEvaluacion_establecimientoInconsistente_lanzaExcepcion() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));

        RegistrarEvaluacionZonificacionDto dto = RegistrarEvaluacionZonificacionDto.builder()
                .establecimientoId(UUID.randomUUID()) // Distinto al del expediente
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .referenciaZonificacion("PDU CC")
                .fuenteSustentoTecnico("Informe Técnico N° 100-2026")
                .observaciones("Motivación técnica detallada superior a 15 caracteres.")
                .build();

        assertThatThrownBy(() -> service.registrarEvaluacion(expedienteId, dto, UUID.randomUUID(), "evaluador"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide con el establecimiento registrado");
    }

    @Test
    @DisplayName("Zonificación: informe técnico o motivación insuficiente es rechazado")
    void registrarEvaluacion_sustentoInsuficiente_lanzaExcepcion() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));

        RegistrarEvaluacionZonificacionDto dto = RegistrarEvaluacionZonificacionDto.builder()
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .referenciaZonificacion("PDU CC")
                .fuenteSustentoTecnico("Inf-1") // Menos de 8 caracteres
                .observaciones("Corta") // Menos de 15 caracteres
                .build();

        assertThatThrownBy(() -> service.registrarEvaluacion(expedienteId, dto, UUID.randomUUID(), "evaluador"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Zonificación BOLA/IDOR: deniega acceso a consulta de expediente ajeno")
    void obtenerUltimaEvaluacion_expedienteAjeno_denegado() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        UUID usuarioAjeno = UUID.randomUUID();

        assertThatThrownBy(() -> service.obtenerUltimaEvaluacion(expedienteId, usuarioAjeno, "99887766", "ROLE_CIUDADANO"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No cuenta con autorización");
    }

    @Test
    @DisplayName("Zonificación: filtra evaluaciones por resultado")
    void listarPorResultado_filtraCorrectamente() {
        EvaluacionZonificacion e1 = EvaluacionZonificacion.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedienteId)
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .build();
        when(zonificacionRepository.findByResultadoOrderByFechaRegistroAsc(ResultadoZonificacion.COMPATIBLE))
                .thenReturn(List.of(e1));

        List<EvaluacionZonificacionDto> lista = service.listarPorResultado(ResultadoZonificacion.COMPATIBLE);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getResultado()).isEqualTo(ResultadoZonificacion.COMPATIBLE);
    }
}
