package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.HistorialTransicionListener;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.TransicionExpedienteRegistrada;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

/**
 * Recalcula la fecha límite del plazo legal después de que la transición quedó registrada en el historial.
 */
@Component
@RequiredArgsConstructor
public class PlazoTransicionListener {

    private final ServicioPlazoLegal servicioPlazoLegal;
    private final ExpedienteRepository expedienteRepository;

    @EventListener
    @Order(HistorialTransicionListener.ORDEN + 10)
    public void alRegistrarTransicion(TransicionExpedienteRegistrada evento) {
        expedienteRepository.findById(evento.transicion().expedienteId())
                .ifPresent(servicioPlazoLegal::actualizarFechaLimite);
    }
}
