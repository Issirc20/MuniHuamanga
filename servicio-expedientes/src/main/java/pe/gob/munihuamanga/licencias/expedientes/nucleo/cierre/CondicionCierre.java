package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.LocalDate;

/**
 * Condición que debe cumplirse para concluir un procedimiento con licencia emitida.
 * Cada condición es independiente; agregar una nueva no exige modificar las existentes.
 */
public interface CondicionCierre {

    ResultadoCondicion evaluar(Expediente expediente, LocalDate fechaEvaluacion);
}
