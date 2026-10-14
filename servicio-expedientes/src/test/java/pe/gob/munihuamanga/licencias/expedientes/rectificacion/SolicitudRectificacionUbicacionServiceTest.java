package pe.gob.munihuamanga.licencias.expedientes.rectificacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pe.gob.munihuamanga.licencias.common.dto.EvaluarRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitarRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitudRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.model.SolicitudRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.repository.SolicitudRectificacionUbicacionRepository;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.service.SolicitudRectificacionUbicacionService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudRectificacionUbicacionServiceTest {

    @Mock
    private SolicitudRectificacionUbicacionRepository solicitudRepository;

    @Mock
    private EstablecimientoRepository establecimientoRepository;

    @Mock
    private pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository expedienteRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SolicitudRectificacionUbicacionService rectificacionService;

    private UUID establecimientoId;
    private Establecimiento establecimiento;

    @BeforeEach
    void setUp() {
        establecimientoId = UUID.randomUUID();
        establecimiento = Establecimiento.builder()
                .id(establecimientoId)
                .nombreComercial("Ferretería El Progreso")
                .direccion("Av. Mariscal Cáceres 450")
                .latitud(new BigDecimal("-13.1630000"))
                .longitud(new BigDecimal("-74.2230000"))
                .build();
    }

    @Test
    @DisplayName("Solicitar rectificación de coordenadas -> Registra PENDIENTE y NO altera coordenadas del establecimiento")
    void solicitarRectificacion_coordenadasInalteradas_exitoso() {
        when(authentication.getName()).thenReturn("ciudadano_juan");
        when(establecimientoRepository.findById(establecimientoId)).thenReturn(Optional.of(establecimiento));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SolicitarRectificacionUbicacionDto dto = SolicitarRectificacionUbicacionDto.builder()
                .establecimientoId(establecimientoId)
                .latitudPropuesta(new BigDecimal("-13.1631000"))
                .longitudPropuesta(new BigDecimal("-74.2231000"))
                .justificacion("Ajuste de precisión por GPS en puerta principal del local.")
                .build();

        SolicitudRectificacionUbicacionDto res = rectificacionService.solicitarRectificacion(dto, authentication);

        assertThat(res).isNotNull();
        assertThat(res.getEstado()).isEqualTo(EstadoRectificacionUbicacion.PENDIENTE);
        assertThat(res.getLatitudOriginal()).isEqualTo(new BigDecimal("-13.1630000"));

        // GARANTÍA: Coordenadas del establecimiento NO se modificaron
        assertThat(establecimiento.getLatitud()).isEqualTo(new BigDecimal("-13.1630000"));
        assertThat(establecimiento.getLongitud()).isEqualTo(new BigDecimal("-74.2230000"));
        verify(establecimientoRepository, never()).save(establecimiento);
    }

    @Test
    @DisplayName("Evaluar rectificación con aprobación humana -> Actualiza coordenadas oficiales del establecimiento")
    void evaluarRectificacion_aprobada_actualizaCoordenadasEstablecimiento() {
        UUID solicitudId = UUID.randomUUID();
        SolicitudRectificacionUbicacion sol = SolicitudRectificacionUbicacion.builder()
                .id(solicitudId)
                .establecimientoId(establecimientoId)
                .latitudOriginal(new BigDecimal("-13.1630000"))
                .longitudOriginal(new BigDecimal("-74.2230000"))
                .latitudPropuesta(new BigDecimal("-13.1631000"))
                .longitudPropuesta(new BigDecimal("-74.2231000"))
                .distanciaMetrosAprox(15.0)
                .estado(EstadoRectificacionUbicacion.PENDIENTE)
                .esTrasladoFisico(false)
                .build();

        when(authentication.getName()).thenReturn("evaluador_muni");
        when(solicitudRepository.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(establecimientoRepository.findById(establecimientoId)).thenReturn(Optional.of(establecimiento));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EvaluarRectificacionUbicacionDto evalDto = EvaluarRectificacionUbicacionDto.builder()
                .aprobado(true)
                .motivoRespuesta("Se constató en cartografía municipal el error de punto georreferenciado.")
                .esTrasladoFisico(false)
                .build();

        SolicitudRectificacionUbicacionDto res = rectificacionService.evaluarRectificacion(solicitudId, evalDto, authentication);

        assertThat(res.getEstado()).isEqualTo(EstadoRectificacionUbicacion.APROBADA);
        // Coordenadas oficiales actualizadas
        assertThat(establecimiento.getLatitud()).isEqualTo(new BigDecimal("-13.1631000"));
        assertThat(establecimiento.getLongitud()).isEqualTo(new BigDecimal("-74.2231000"));
        verify(establecimientoRepository).save(establecimiento);
    }

    @Test
    @DisplayName("Evaluar rectificación que constituye traslado físico -> Rechaza y no altera coordenadas")
    void evaluarRectificacion_esTrasladoFisico_rechazaYNoActualiza() {
        UUID solicitudId = UUID.randomUUID();
        SolicitudRectificacionUbicacion sol = SolicitudRectificacionUbicacion.builder()
                .id(solicitudId)
                .establecimientoId(establecimientoId)
                .latitudOriginal(new BigDecimal("-13.1630000"))
                .longitudOriginal(new BigDecimal("-74.2230000"))
                .latitudPropuesta(new BigDecimal("-13.1900000"))
                .longitudPropuesta(new BigDecimal("-74.2500000"))
                .distanciaMetrosAprox(4200.0)
                .estado(EstadoRectificacionUbicacion.PENDIENTE)
                .esTrasladoFisico(true)
                .build();

        when(authentication.getName()).thenReturn("evaluador_muni");
        when(solicitudRepository.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(establecimientoRepository.findById(establecimientoId)).thenReturn(Optional.of(establecimiento));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EvaluarRectificacionUbicacionDto evalDto = EvaluarRectificacionUbicacionDto.builder()
                .aprobado(false)
                .motivoRespuesta("La distancia supera los 4 km. Constituye un traslado físico de establecimiento comercial. Debe tramitar nueva licencia.")
                .esTrasladoFisico(true)
                .build();

        SolicitudRectificacionUbicacionDto res = rectificacionService.evaluarRectificacion(solicitudId, evalDto, authentication);

        assertThat(res.getEstado()).isEqualTo(EstadoRectificacionUbicacion.RECHAZADA);
        assertThat(res.getEsTrasladoFisico()).isTrue();
        // Coordenadas originales intactas
        assertThat(establecimiento.getLatitud()).isEqualTo(new BigDecimal("-13.1630000"));
        assertThat(establecimiento.getLongitud()).isEqualTo(new BigDecimal("-74.2230000"));
        verify(establecimientoRepository, never()).save(establecimiento);
    }

    @Test
    @DisplayName("Evaluar rectificación con distancia > 300m aprobada motivadamente por evaluador -> Se aprueba válidamente")
    void evaluarRectificacion_distanciaMayor300mPeroEvaluadorAprueba_aprobadaExitosamente() {
        UUID solicitudId = UUID.randomUUID();
        SolicitudRectificacionUbicacion sol = SolicitudRectificacionUbicacion.builder()
                .id(solicitudId)
                .establecimientoId(establecimientoId)
                .latitudOriginal(new BigDecimal("-13.1630000"))
                .longitudOriginal(new BigDecimal("-74.2230000"))
                .latitudPropuesta(new BigDecimal("-13.1660000"))
                .longitudPropuesta(new BigDecimal("-74.2260000"))
                .distanciaMetrosAprox(450.0) // Mayor a 300m
                .estado(EstadoRectificacionUbicacion.PENDIENTE)
                .esTrasladoFisico(true) // Alerta inicial por distancia
                .build();

        when(authentication.getName()).thenReturn("evaluador_muni");
        when(solicitudRepository.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(establecimientoRepository.findById(establecimientoId)).thenReturn(Optional.of(establecimiento));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Evaluador constata que fue un error tipográfico de coordenadas y NO un traslado físico
        EvaluarRectificacionUbicacionDto evalDto = EvaluarRectificacionUbicacionDto.builder()
                .aprobado(true)
                .motivoRespuesta("Se constata mediante inspección y plano catastral que el inmueble siempre operó aquí. Se subsana error de tipeo GPS inicial.")
                .esTrasladoFisico(false)
                .build();

        SolicitudRectificacionUbicacionDto res = rectificacionService.evaluarRectificacion(solicitudId, evalDto, authentication);

        assertThat(res.getEstado()).isEqualTo(EstadoRectificacionUbicacion.APROBADA);
        assertThat(res.getEsTrasladoFisico()).isFalse();
        assertThat(establecimiento.getLatitud()).isEqualTo(new BigDecimal("-13.1660000"));
        assertThat(establecimiento.getLongitud()).isEqualTo(new BigDecimal("-74.2260000"));
        verify(establecimientoRepository).save(establecimiento);
    }

    @Test
    @DisplayName("Solicitar rectificación indicando expediente de otro establecimiento -> Lanza excepción")
    void solicitarRectificacion_expedienteAjeno_lanzaExcepcion() {
        UUID expedienteAjenoId = UUID.randomUUID();
        UUID otroEstablecimientoId = UUID.randomUUID();

        pe.gob.munihuamanga.licencias.expedientes.model.Expediente expAjeno = pe.gob.munihuamanga.licencias.expedientes.model.Expediente.builder()
                .id(expedienteAjenoId)
                .establecimientoId(otroEstablecimientoId)
                .build();

        when(establecimientoRepository.findById(establecimientoId)).thenReturn(Optional.of(establecimiento));
        when(expedienteRepository.findById(expedienteAjenoId)).thenReturn(Optional.of(expAjeno));

        SolicitarRectificacionUbicacionDto dto = SolicitarRectificacionUbicacionDto.builder()
                .establecimientoId(establecimientoId)
                .expedienteId(expedienteAjenoId)
                .latitudPropuesta(new BigDecimal("-13.1631000"))
                .longitudPropuesta(new BigDecimal("-74.2231000"))
                .justificacion("Corrección con expediente ajeno")
                .build();

        assertThatThrownBy(() -> rectificacionService.solicitarRectificacion(dto, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no corresponde al establecimiento");
    }
}
