package pe.gob.munihuamanga.licencias.expedientes.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inicializador de datos semilla para agilizar pruebas en entornos de desarrollo local.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ExpedienteRepository expedienteRepository;
    private final AuditoriaService auditoriaService;

    @Override
    public void run(String... args) {
        if (expedienteRepository.count() == 0) {
            log.info("Inicializando datos de prueba en la base de datos...");

            UUID id = UUID.randomUUID();
            Expediente exp = Expediente.builder()
                    .id(id)
                    .numeroTramite("EXP-2026-00001")
                    .solicitanteId(UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"))
                    .nombreTitular("María Quispe Huamán")
                    .documentoIdentidad("42567891")
                    .razonSocial("INVERSIONES LOS RETABLOS S.A.C.")
                    .nombreComercial("Boutique Artesanal Huamanga")
                    .giroNegocio("Venta de artesanías, retablos y souvenirs")
                    .direccionEstablecimiento("Jr. 9 de Diciembre N° 142, Centro Histórico, Ayacucho")
                    .areaMetrosCuadrados(new BigDecimal("35.50"))
                    .estado(EstadoExpediente.FORMATOS_GENERADOS)
                    .nivelRiesgo(NivelRiesgo.BAJO)
                    .montoTasa(new BigDecimal("154.50"))
                    .fechaCreacion(LocalDateTime.now())
                    .fechaLimite(LocalDateTime.now().plusDays(21))
                    .build();

            expedienteRepository.save(exp);

            auditoriaService.registrarTransicion(
                    id,
                    null,
                    EstadoExpediente.FORMATOS_GENERADOS,
                    "María Quispe Huamán",
                    "Ingreso de solicitud inicial de prueba"
            );

            log.info("Expediente de prueba creado: EXP-2026-00001 (ID: {})", id);
        }
    }
}
