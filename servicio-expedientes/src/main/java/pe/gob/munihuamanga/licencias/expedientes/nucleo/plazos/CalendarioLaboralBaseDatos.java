package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Calendario laboral respaldado por la tabla de días no laborables.
 * Mantiene el conjunto de fechas en memoria; se recarga al modificarse el calendario.
 */
@Component
@RequiredArgsConstructor
public class CalendarioLaboralBaseDatos implements CalendarioLaboral {

    private final DiaNoLaborableRepository repositorio;
    private final AtomicReference<Set<LocalDate>> diasNoLaborables = new AtomicReference<>();

    @Override
    public boolean esDiaHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            return false;
        }
        return !fechasNoLaborables().contains(fecha);
    }

    /** Descarta el conjunto en memoria para que la próxima consulta lo lea nuevamente. */
    public void refrescar() {
        diasNoLaborables.set(null);
    }

    private Set<LocalDate> fechasNoLaborables() {
        Set<LocalDate> actuales = diasNoLaborables.get();
        if (actuales == null) {
            actuales = repositorio.findAll().stream()
                    .map(DiaNoLaborable::getFecha)
                    .collect(Collectors.toUnmodifiableSet());
            diasNoLaborables.set(actuales);
        }
        return actuales;
    }
}
