package pe.gob.munihuamanga.licencias.expedientes.pagos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.dto.LiquidacionDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.service.CalculadoraDeTasa;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Liquidación de la tasa con el tarifario TUPA vigente.
 */
@Component
@RequiredArgsConstructor
public class LiquidadorTasaTupa implements LiquidadorTasa {

    private final CalculadoraDeTasa calculadoraDeTasa;
    private final Clock reloj;

    @Override
    public LiquidacionDto liquidar(NivelRiesgo nivelRiesgo, boolean definitiva) {
        if (nivelRiesgo == null) {
            throw new IllegalArgumentException("El nivel de riesgo es obligatorio para liquidar la tasa.");
        }
        return LiquidacionDto.builder()
                .nivelRiesgo(nivelRiesgo)
                .monto(calculadoraDeTasa.calcularTasa(nivelRiesgo))
                .concepto("Tasa de licencia de funcionamiento - nivel de riesgo " + nivelRiesgo.name().replace('_', ' '))
                .fechaLiquidacion(LocalDateTime.now(reloj))
                .definitiva(definitiva)
                .build();
    }
}
