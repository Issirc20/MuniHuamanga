package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Aritmética de días hábiles. El día de inicio no se computa (TUO LPAG, art. 145):
 * el plazo empieza a correr al día hábil siguiente.
 */
@Component
@RequiredArgsConstructor
public class CalculadoraPlazos {

    private final CalendarioLaboral calendario;

    /** Fecha en que se cumple el plazo de {@code dias} días hábiles contados desde {@code inicio}. */
    public LocalDate sumarDiasHabiles(LocalDate inicio, int dias) {
        if (dias < 0) {
            throw new IllegalArgumentException("El número de días hábiles no puede ser negativo.");
        }
        LocalDate cursor = inicio;
        int contados = 0;
        while (contados < dias) {
            cursor = cursor.plusDays(1);
            if (calendario.esDiaHabil(cursor)) {
                contados++;
            }
        }
        return cursor;
    }

    /** Días hábiles transcurridos después de {@code desde} hasta {@code hasta}, inclusive. */
    public int contarDiasHabiles(LocalDate desde, LocalDate hasta) {
        if (!hasta.isAfter(desde)) {
            return 0;
        }
        int contados = 0;
        LocalDate cursor = desde.plusDays(1);
        while (!cursor.isAfter(hasta)) {
            if (calendario.esDiaHabil(cursor)) {
                contados++;
            }
            cursor = cursor.plusDays(1);
        }
        return contados;
    }
}
