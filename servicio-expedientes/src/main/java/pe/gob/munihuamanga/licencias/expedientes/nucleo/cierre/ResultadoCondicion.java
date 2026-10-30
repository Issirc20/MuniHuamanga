package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

/**
 * Resultado de la evaluación de una condición de cierre.
 */
public record ResultadoCondicion(String codigo, boolean cumplida, String detalle) {

    public static ResultadoCondicion cumplida(String codigo, String detalle) {
        return new ResultadoCondicion(codigo, true, detalle);
    }

    public static ResultadoCondicion pendiente(String codigo, String detalle) {
        return new ResultadoCondicion(codigo, false, detalle);
    }
}
