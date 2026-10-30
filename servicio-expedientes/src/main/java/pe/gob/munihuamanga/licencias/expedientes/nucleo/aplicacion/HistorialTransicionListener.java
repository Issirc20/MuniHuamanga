package pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

/**
 * Registra cada transición en el historial inmutable, dentro de la misma transacción
 * que la modificó, para que estado e historial nunca queden desalineados.
 */
@Component
@RequiredArgsConstructor
public class HistorialTransicionListener {

    public static final int ORDEN = 0;

    private final AuditoriaService auditoriaService;

    @EventListener
    @Order(ORDEN)
    public void alRegistrarTransicion(TransicionExpedienteRegistrada evento) {
        auditoriaService.registrarTransicion(evento.transicion());
    }
}
