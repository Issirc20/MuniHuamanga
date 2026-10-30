package pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio;

import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cambio de estado efectivamente aplicado sobre un expediente.
 */
public record TransicionExpediente(
        UUID expedienteId,
        String numeroTramite,
        EstadoExpediente estadoAnterior,
        EstadoExpediente estadoNuevo,
        EventoExpediente evento,
        Actor actor,
        LocalDateTime momento,
        String sustento
) {
}
