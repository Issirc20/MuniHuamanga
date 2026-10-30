package pe.gob.munihuamanga.licencias.expedientes.itse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.HistorialTransicionListener;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.TransicionExpedienteRegistrada;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;

/**
 * Abre la etapa ITSE cuando la solicitud es admitida.
 */
@Component
@RequiredArgsConstructor
public class EtapaItseListener {

    private final ItseService itseService;

    @EventListener
    @Order(HistorialTransicionListener.ORDEN + 20)
    public void alRegistrarTransicion(TransicionExpedienteRegistrada evento) {
        if (evento.transicion().evento() == EventoExpediente.SOLICITUD_ADMITIDA) {
            itseService.iniciarEtapa(evento.transicion().expedienteId());
        }
    }
}
