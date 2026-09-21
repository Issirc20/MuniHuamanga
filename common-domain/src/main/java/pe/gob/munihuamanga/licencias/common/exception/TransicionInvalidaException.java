package pe.gob.munihuamanga.licencias.common.exception;

import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;

public class TransicionInvalidaException extends RuntimeException {

    private final EstadoExpediente estadoActual;
    private final EstadoExpediente estadoDestino;

    public TransicionInvalidaException(EstadoExpediente estadoActual, EstadoExpediente estadoDestino) {
        super(String.format("Transición de estado no permitida: no se puede pasar de '%s' a '%s'.", estadoActual, estadoDestino));
        this.estadoActual = estadoActual;
        this.estadoDestino = estadoDestino;
    }

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
        this.estadoActual = null;
        this.estadoDestino = null;
    }

    public EstadoExpediente getEstadoActual() {
        return estadoActual;
    }

    public EstadoExpediente getEstadoDestino() {
        return estadoDestino;
    }
}
