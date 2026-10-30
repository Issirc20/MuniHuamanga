package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registra los feriados nacionales del año en curso y del siguiente cuando aún no existen.
 * La administración puede corregirlos o agregar días no laborables decretados desde el calendario institucional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeriadosNacionalesInitializer implements ApplicationRunner {

    private static final String BASE_LEGAL = "Decreto Legislativo N° 713 y normas modificatorias";
    private static final Map<MonthDay, String> FERIADOS_FIJOS = new LinkedHashMap<>();

    static {
        FERIADOS_FIJOS.put(MonthDay.of(1, 1), "Año Nuevo");
        FERIADOS_FIJOS.put(MonthDay.of(5, 1), "Día del Trabajo");
        FERIADOS_FIJOS.put(MonthDay.of(6, 7), "Batalla de Arica y Día de la Bandera");
        FERIADOS_FIJOS.put(MonthDay.of(6, 29), "San Pedro y San Pablo");
        FERIADOS_FIJOS.put(MonthDay.of(7, 23), "Día de la Fuerza Aérea del Perú");
        FERIADOS_FIJOS.put(MonthDay.of(7, 28), "Fiestas Patrias");
        FERIADOS_FIJOS.put(MonthDay.of(7, 29), "Fiestas Patrias");
        FERIADOS_FIJOS.put(MonthDay.of(8, 6), "Batalla de Junín");
        FERIADOS_FIJOS.put(MonthDay.of(8, 30), "Santa Rosa de Lima");
        FERIADOS_FIJOS.put(MonthDay.of(10, 8), "Combate de Angamos");
        FERIADOS_FIJOS.put(MonthDay.of(11, 1), "Día de Todos los Santos");
        FERIADOS_FIJOS.put(MonthDay.of(12, 8), "Inmaculada Concepción");
        FERIADOS_FIJOS.put(MonthDay.of(12, 9), "Batalla de Ayacucho");
        FERIADOS_FIJOS.put(MonthDay.of(12, 25), "Navidad");
    }

    private final DiaNoLaborableRepository repositorio;
    private final CalendarioLaboralBaseDatos calendario;
    private final Clock reloj;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int anioActual = LocalDate.now(reloj).getYear();
        int registrados = registrarAnio(anioActual) + registrarAnio(anioActual + 1);
        if (registrados > 0) {
            calendario.refrescar();
            log.info("[CALENDARIO] Se registraron {} feriados nacionales para {} y {}.", registrados, anioActual, anioActual + 1);
        }
    }

    private int registrarAnio(int anio) {
        Map<LocalDate, String> feriados = new LinkedHashMap<>();
        FERIADOS_FIJOS.forEach((diaMes, descripcion) -> feriados.put(diaMes.atYear(anio), descripcion));
        LocalDate pascua = domingoDePascua(anio);
        feriados.put(pascua.minusDays(3), "Jueves Santo");
        feriados.put(pascua.minusDays(2), "Viernes Santo");

        int registrados = 0;
        for (Map.Entry<LocalDate, String> feriado : feriados.entrySet()) {
            if (!repositorio.existsById(feriado.getKey())) {
                repositorio.save(DiaNoLaborable.builder()
                        .fecha(feriado.getKey())
                        .descripcion(feriado.getValue())
                        .baseLegal(BASE_LEGAL)
                        .registradoPor("SISTEMA")
                        .fechaRegistro(LocalDateTime.now(reloj))
                        .build());
                registrados++;
            }
        }
        return registrados;
    }

    /** Cómputo gregoriano del domingo de Pascua (algoritmo de Meeus/Jones/Butcher). */
    static LocalDate domingoDePascua(int anio) {
        int a = anio % 19;
        int b = anio / 100;
        int c = anio % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }
}
