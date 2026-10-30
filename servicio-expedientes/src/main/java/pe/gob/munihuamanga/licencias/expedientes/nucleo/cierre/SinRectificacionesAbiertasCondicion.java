package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.repository.SolicitudRectificacionUbicacionRepository;

import java.time.LocalDate;

/**
 * No debe existir ninguna solicitud de rectificación de ubicación pendiente sobre el expediente.
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class SinRectificacionesAbiertasCondicion implements CondicionCierre {

    static final String CODIGO = "SIN_RECTIFICACIONES_ABIERTAS";

    private final SolicitudRectificacionUbicacionRepository rectificacionRepository;

    @Override
    public ResultadoCondicion evaluar(Expediente expediente, LocalDate fechaEvaluacion) {
        return rectificacionRepository.existsByExpedienteIdAndEstado(expediente.getId(), EstadoRectificacionUbicacion.PENDIENTE)
                ? ResultadoCondicion.pendiente(CODIGO, "Existe una solicitud de rectificación de ubicación pendiente.")
                : ResultadoCondicion.cumplida(CODIGO, "No existen rectificaciones de ubicación pendientes.");
    }
}
