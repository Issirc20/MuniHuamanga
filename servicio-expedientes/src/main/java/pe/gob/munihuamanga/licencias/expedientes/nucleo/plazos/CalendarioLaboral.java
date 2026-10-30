package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import java.time.LocalDate;

/**
 * Puerto que determina si una fecha es hábil para el cómputo de plazos administrativos.
 */
public interface CalendarioLaboral {

    boolean esDiaHabil(LocalDate fecha);
}
