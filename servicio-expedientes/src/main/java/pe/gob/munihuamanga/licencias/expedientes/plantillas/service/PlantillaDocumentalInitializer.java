package pe.gob.munihuamanga.licencias.expedientes.plantillas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.model.PlantillaDocumental;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.model.PlantillaDocumentalVersion;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.repository.PlantillaDocumentalRepository;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.repository.PlantillaDocumentalVersionRepository;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Inicializador que asegura el sembrado inicial y la integridad de las 5 plantillas oficiales.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class PlantillaDocumentalInitializer implements CommandLineRunner {

    private final PlantillaDocumentalRepository plantillaRepository;
    private final PlantillaDocumentalVersionRepository versionRepository;
    private final PlantillaDocumentalService plantillaService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Verificando existencia e integridad de plantillas documentales oficiales...");

        seedPlantilla(
                "ITSE_ANEXO_1",
                "Anexo 1 ITSE — Solicitud de Inspección Técnica de Seguridad en Edificaciones (ITSE / ECSE)",
                "Solicitud oficial de ITSE previa, posterior o ECSE según D.S. N° 002-2018-PCM / R.J. N° 016-2018-CENEPRED/J.",
                "D.S. N° 002-2018-PCM / R.J. N° 016-2018-CENEPRED/J",
                "ITSE",
                "itse-anexo1-plantilla.pdf"
        );

        seedPlantilla(
                "ITSE_ANEXO_2",
                "Anexo 2 ITSE — Información para Determinación del Nivel de Riesgo",
                "Matriz de características declaradas del establecimiento (salud, encuentro, hospedaje, educación, industria, comercio, almacén y factores adicionales).",
                "R.J. N° 016-2018-CENEPRED/J - Manual de Ejecución ITSE",
                "ITSE",
                "itse-anexo2-plantilla.pdf"
        );

        seedPlantilla(
                "ITSE_ANEXO_3",
                "Anexo 3 ITSE — Reporte de Nivel de Riesgo del Establecimiento Objeto de Inspección",
                "Reporte de clasificación de riesgo de incendio y colapso (Bajo, Medio, Alto, Muy Alto). Clasificación preliminar informativa y oficial por el inspector.",
                "D.S. N° 002-2018-PCM / Matriz de Riesgo CENEPRED",
                "ITSE",
                "itse-anexo3-plantilla.pdf"
        );

        seedPlantilla(
                "ITSE_ANEXO_4",
                "Anexo 4 ITSE — Declaración Jurada de Cumplimiento de Condiciones de Seguridad",
                "Declaración jurada de 4 páginas de verificación de condiciones de seguridad en la edificación para establecimientos con ITSE posterior.",
                "D.S. N° 002-2018-PCM / R.J. N° 016-2018-CENEPRED/J",
                "ITSE",
                "itse-anexo4-plantilla.pdf"
        );

        seedPlantilla(
                "LICENCIA_ANEXO_1",
                "Anexo N.º 1 — Declaración Jurada para Licencia de Funcionamiento",
                "Formato oficial de declaración jurada para trámite de licencia de funcionamiento conforme al TUO de la Ley N.º 28976, versión 03.",
                "Ley N° 28976 / D.S. N° 046-2017-PCM / D.S. N° 163-2020-PCM",
                "LICENCIA",
                "licencia-anexo1-plantilla.pdf"
        );

        log.info("Sembrado de plantillas documentales oficiales completado.");
    }

    private void seedPlantilla(String codigo, String nombre, String descripcion, String baseLegal, String grupo, String archivoClasspath) {
        Optional<PlantillaDocumental> existente = plantillaRepository.findByCodigo(codigo);
        if (existente.isPresent()) {
            return;
        }

        try {
            ClassPathResource res = new ClassPathResource("templates/pdf/" + archivoClasspath);
            if (!res.exists()) {
                log.warn("Archivo de plantilla {} no encontrado en classpath: templates/pdf/{}", codigo, archivoClasspath);
                return;
            }

            byte[] bytes;
            try (InputStream is = res.getInputStream()) {
                bytes = is.readAllBytes();
            }

            String sha256 = plantillaService.calcularSha256(bytes);
            UUID plantillaId = UUID.randomUUID();
            int versionInicial = 1;
            String versionTexto = "v1.0 (2026)";

            PlantillaDocumental plantilla = PlantillaDocumental.builder()
                    .id(plantillaId)
                    .codigo(codigo)
                    .nombre(nombre)
                    .descripcion(descripcion)
                    .baseLegal(baseLegal)
                    .grupo(grupo)
                    .versionActual(versionInicial)
                    .versionTexto(versionTexto)
                    .nombreArchivo(archivoClasspath)
                    .hashSha256(sha256)
                    .tamanioBytes((long) bytes.length)
                    .rutaAlmacenamiento("") // Almacenado en classpath inicial
                    .fechaActualizacion(LocalDateTime.now())
                    .actualizadoPor("SISTEMA_INICIAL")
                    .activo(true)
                    .build();

            plantillaRepository.save(plantilla);

            PlantillaDocumentalVersion version1 = PlantillaDocumentalVersion.builder()
                    .id(UUID.randomUUID())
                    .plantillaId(plantillaId)
                    .codigoPlantilla(codigo)
                    .version(versionInicial)
                    .versionTexto(versionTexto)
                    .nombreArchivo(archivoClasspath)
                    .hashSha256(sha256)
                    .tamanioBytes((long) bytes.length)
                    .rutaAlmacenamiento("")
                    .motivoCambio("Versión base oficial aprobada y calibrada según normativa vigente")
                    .fechaCreacion(LocalDateTime.now())
                    .creadoPor("SISTEMA_INICIAL")
                    .esVersionVigente(true)
                    .build();

            versionRepository.save(version1);
            log.info("Plantilla {} sembrada exitosamente con hash SHA-256: {}", codigo, sha256);

        } catch (Exception e) {
            log.error("Error al sembrar plantilla {}", codigo, e);
        }
    }
}
