package pe.gob.munihuamanga.licencias.common.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String entidad, Object identificador) {
        super(String.format("No se encontró el recurso %s con identificador: %s", entidad, identificador));
    }
}
