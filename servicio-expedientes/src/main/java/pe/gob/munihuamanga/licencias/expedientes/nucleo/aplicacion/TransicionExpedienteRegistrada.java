package pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion;

import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.TransicionExpediente;

/**
 * Evento de aplicación publicado cada vez que el expediente cambia de estado.
 * Los oyentes síncronos se ejecutan dentro de la misma transacción.
 */
public record TransicionExpedienteRegistrada(TransicionExpediente transicion) {
}
