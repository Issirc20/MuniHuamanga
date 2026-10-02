package pe.gob.munihuamanga.licencias.expedientes.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.model.TarifaTupa;
import pe.gob.munihuamanga.licencias.expedientes.repository.TarifaTupaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Precarga de tarifas del TUPA municipal oficial si no existen en la base de datos (Fase 04 Sprint 4-D).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TarifaTupaDataInitializer implements CommandLineRunner {

    private final TarifaTupaRepository tarifaRepository;

    @Override
    public void run(String... args) {
        crearTarifaSiNoExiste(
                "TUPA-ITSE-01",
                NivelRiesgo.BAJO,
                "Licencia de Funcionamiento con Inspección Técnica de Seguridad en Edificaciones Posterior (Riesgo Bajo)",
                new BigDecimal("154.50"),
                new BigDecimal("45.00"),
                new BigDecimal("109.50"),
                "Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM"
        );

        crearTarifaSiNoExiste(
                "TUPA-ITSE-02",
                NivelRiesgo.MEDIO,
                "Licencia de Funcionamiento con Inspección Técnica de Seguridad en Edificaciones Posterior (Riesgo Medio)",
                new BigDecimal("218.00"),
                new BigDecimal("45.00"),
                new BigDecimal("173.00"),
                "Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM"
        );

        crearTarifaSiNoExiste(
                "TUPA-ITSE-03",
                NivelRiesgo.ALTO,
                "Licencia de Funcionamiento con Inspección Técnica de Seguridad en Edificaciones Previa (Riesgo Alto)",
                new BigDecimal("345.20"),
                new BigDecimal("45.00"),
                new BigDecimal("300.20"),
                "Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM"
        );

        crearTarifaSiNoExiste(
                "TUPA-ITSE-04",
                NivelRiesgo.MUY_ALTO,
                "Licencia de Funcionamiento con Inspección Técnica de Seguridad en Edificaciones Previa (Riesgo Muy Alto)",
                new BigDecimal("480.00"),
                new BigDecimal("45.00"),
                new BigDecimal("435.00"),
                "Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM"
        );
    }

    private void crearTarifaSiNoExiste(
            String codigoTupa,
            NivelRiesgo nivelRiesgo,
            String concepto,
            BigDecimal montoTotal,
            BigDecimal derechoTramite,
            BigDecimal costoItse,
            String baseLegal
    ) {
        if (tarifaRepository.findByNivelRiesgo(nivelRiesgo).isEmpty()) {
            TarifaTupa tarifa = TarifaTupa.builder()
                    .id(UUID.randomUUID())
                    .codigoTupa(codigoTupa)
                    .nivelRiesgo(nivelRiesgo)
                    .concepto(concepto)
                    .montoTotal(montoTotal)
                    .derechoTramite(derechoTramite)
                    .costoItse(costoItse)
                    .baseLegal(baseLegal)
                    .activo(true)
                    .fechaActualizacion(LocalDateTime.now())
                    .usuarioModificacion("SISTEMA_INICIALIZADOR")
                    .build();

            tarifaRepository.save(tarifa);
            log.info("[TUPA] Tarifa oficial precargada: {} ({}) = S/. {}", codigoTupa, nivelRiesgo, montoTotal);
        }
    }
}
