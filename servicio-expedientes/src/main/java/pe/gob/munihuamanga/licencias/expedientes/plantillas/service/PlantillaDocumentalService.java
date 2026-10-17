package pe.gob.munihuamanga.licencias.expedientes.plantillas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.PlantillaDocumentalDto;
import pe.gob.munihuamanga.licencias.common.dto.SustituirPlantillaDto;
import pe.gob.munihuamanga.licencias.common.dto.VersionPlantillaDto;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.model.PlantillaDocumental;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.model.PlantillaDocumentalVersion;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.repository.PlantillaDocumentalRepository;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.repository.PlantillaDocumentalVersionRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión, versionado inmutable y auditoría de plantillas documentales oficiales.
 * Cumple con RNF-20, trazabilidad SHA-256 y no retroactividad documental.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaDocumentalService {

    private final PlantillaDocumentalRepository plantillaRepository;
    private final PlantillaDocumentalVersionRepository versionRepository;

    @Value("${app.plantillas.almacenamiento-dir:./data/plantillas}")
    private String almacenamientoDir;

    @Transactional(readOnly = true)
    public List<PlantillaDocumentalDto> listarPlantillas() {
        return plantillaRepository.findByActivoTrueOrderByCodigoAsc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlantillaDocumentalDto> listarPlantillasPorGrupo(String grupo) {
        return plantillaRepository.findByGrupoAndActivoTrueOrderByCodigoAsc(grupo.toUpperCase()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlantillaDocumentalDto obtenerPlantillaPorCodigo(String codigo) {
        PlantillaDocumental p = plantillaRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada con código: " + codigo));
        return mapToDto(p);
    }

    @Transactional(readOnly = true)
    public byte[] obtenerContenidoPlantillaVigente(String codigo) {
        PlantillaDocumental p = plantillaRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada con código: " + codigo));
        return leerBytesPlantilla(p.getRutaAlmacenamiento(), p.getNombreArchivo());
    }

    @Transactional(readOnly = true)
    public List<VersionPlantillaDto> listarHistorialVersiones(String codigo) {
        return versionRepository.findByCodigoPlantillaOrderByVersionDesc(codigo.toUpperCase()).stream()
                .map(this::mapVersionToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] obtenerContenidoVersion(String codigo, Integer version) {
        PlantillaDocumental p = plantillaRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada: " + codigo));

        PlantillaDocumentalVersion pv = versionRepository.findByPlantillaIdAndVersion(p.getId(), version)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión " + version + " no encontrada para plantilla " + codigo));

        return leerBytesPlantilla(pv.getRutaAlmacenamiento(), pv.getNombreArchivo());
    }

    @Transactional
    public PlantillaDocumentalDto sustituirPlantilla(String codigo, SustituirPlantillaDto dto, String usuarioActual) {
        PlantillaDocumental plantilla = plantillaRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada: " + codigo));

        byte[] archivoBytes = decodificarYValidarPdf(dto.getContenidoBase64(), dto.getNombreArchivo());
        String sha256 = calcularSha256(archivoBytes);

        int nuevaVersion = plantilla.getVersionActual() + 1;
        String nuevoTextoVersion = "v" + nuevaVersion + ".0 (" + LocalDateTime.now().getYear() + ")";

        // Guardar archivo en almacenamiento físico
        String rutaGuardada = guardarArchivoFisico(codigo, nuevaVersion, sha256, dto.getNombreArchivo(), archivoBytes);

        // Desmarcar versiones vigentes previas
        List<PlantillaDocumentalVersion> previas = versionRepository.findByPlantillaIdOrderByVersionDesc(plantilla.getId());
        for (PlantillaDocumentalVersion v : previas) {
            if (Boolean.TRUE.equals(v.getEsVersionVigente())) {
                v.setEsVersionVigente(false);
                versionRepository.save(v);
            }
        }

        // Crear registro de versión inmutable
        PlantillaDocumentalVersion versionNueva = PlantillaDocumentalVersion.builder()
                .id(UUID.randomUUID())
                .plantillaId(plantilla.getId())
                .codigoPlantilla(plantilla.getCodigo())
                .version(nuevaVersion)
                .versionTexto(nuevoTextoVersion)
                .nombreArchivo(dto.getNombreArchivo())
                .hashSha256(sha256)
                .tamanioBytes((long) archivoBytes.length)
                .rutaAlmacenamiento(rutaGuardada)
                .motivoCambio(dto.getMotivoCambio())
                .fechaCreacion(LocalDateTime.now())
                .creadoPor(usuarioActual != null ? usuarioActual : "SISTEMA")
                .esVersionVigente(true)
                .build();
        versionRepository.save(versionNueva);

        // Actualizar cabecera de plantilla
        plantilla.setVersionActual(nuevaVersion);
        plantilla.setVersionTexto(nuevoTextoVersion);
        plantilla.setNombreArchivo(dto.getNombreArchivo());
        plantilla.setHashSha256(sha256);
        plantilla.setTamanioBytes((long) archivoBytes.length);
        plantilla.setRutaAlmacenamiento(rutaGuardada);
        plantilla.setFechaActualizacion(LocalDateTime.now());
        plantilla.setActualizadoPor(usuarioActual != null ? usuarioActual : "SISTEMA");

        PlantillaDocumental guardada = plantillaRepository.save(plantilla);
        log.info("Plantilla {} actualizada exitosamente a la versión {} por {}", codigo, nuevoTextoVersion, usuarioActual);

        return mapToDto(guardada);
    }

    @Transactional
    public PlantillaDocumentalDto restaurarVersion(String codigo, Integer versionHistorica, String motivo, String usuarioActual) {
        PlantillaDocumental plantilla = plantillaRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada: " + codigo));

        PlantillaDocumentalVersion vHistorica = versionRepository.findByPlantillaIdAndVersion(plantilla.getId(), versionHistorica)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión histórica " + versionHistorica + " no existe"));

        byte[] archivoBytes = leerBytesPlantilla(vHistorica.getRutaAlmacenamiento(), vHistorica.getNombreArchivo());
        String sha256 = calcularSha256(archivoBytes);

        // La restauración no borra el historial: crea una nueva versión controlada
        int nuevaVersion = plantilla.getVersionActual() + 1;
        String nuevoTextoVersion = "v" + nuevaVersion + ".0 (Restauración de v" + versionHistorica + ".0)";

        String motivoDetallado = (motivo != null && !motivo.isBlank())
                ? "Restauración de v" + versionHistorica + ".0: " + motivo
                : "Restauración controlada a partir de la versión v" + versionHistorica + ".0";

        String rutaGuardada = guardarArchivoFisico(codigo, nuevaVersion, sha256, vHistorica.getNombreArchivo(), archivoBytes);

        // Desmarcar versiones vigentes previas
        List<PlantillaDocumentalVersion> previas = versionRepository.findByPlantillaIdOrderByVersionDesc(plantilla.getId());
        for (PlantillaDocumentalVersion v : previas) {
            if (Boolean.TRUE.equals(v.getEsVersionVigente())) {
                v.setEsVersionVigente(false);
                versionRepository.save(v);
            }
        }

        PlantillaDocumentalVersion versionNueva = PlantillaDocumentalVersion.builder()
                .id(UUID.randomUUID())
                .plantillaId(plantilla.getId())
                .codigoPlantilla(plantilla.getCodigo())
                .version(nuevaVersion)
                .versionTexto(nuevoTextoVersion)
                .nombreArchivo(vHistorica.getNombreArchivo())
                .hashSha256(sha256)
                .tamanioBytes((long) archivoBytes.length)
                .rutaAlmacenamiento(rutaGuardada)
                .motivoCambio(motivoDetallado)
                .fechaCreacion(LocalDateTime.now())
                .creadoPor(usuarioActual != null ? usuarioActual : "SISTEMA")
                .esVersionVigente(true)
                .build();
        versionRepository.save(versionNueva);

        plantilla.setVersionActual(nuevaVersion);
        plantilla.setVersionTexto(nuevoTextoVersion);
        plantilla.setNombreArchivo(vHistorica.getNombreArchivo());
        plantilla.setHashSha256(sha256);
        plantilla.setTamanioBytes((long) archivoBytes.length);
        plantilla.setRutaAlmacenamiento(rutaGuardada);
        plantilla.setFechaActualizacion(LocalDateTime.now());
        plantilla.setActualizadoPor(usuarioActual != null ? usuarioActual : "SISTEMA");

        PlantillaDocumental guardada = plantillaRepository.save(plantilla);
        log.info("Plantilla {} restaurada a v{} mediante nueva versión controlada {} por {}",
                codigo, versionHistorica, nuevoTextoVersion, usuarioActual);

        return mapToDto(guardada);
    }

    public byte[] decodificarYValidarPdf(String base64Data, String nombreArchivo) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException("El archivo base64 no puede estar vacío");
        }
        String cleanBase64 = base64Data;
        if (base64Data.contains(",")) {
            cleanBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(cleanBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El contenido no es una codificación Base64 válida");
        }

        // Validación de magic number de PDF: %PDF- (0x25, 0x50, 0x44, 0x46, 0x2D)
        if (bytes.length < 5 || bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46 || bytes[4] != 0x2D) {
            throw new IllegalArgumentException("El archivo subido no es un PDF auténtico (cabecera inválida)");
        }

        return bytes;
    }

    public String calcularSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible", e);
        }
    }

    private byte[] leerBytesPlantilla(String rutaAlmacenamiento, String nombreArchivo) {
        try {
            if (rutaAlmacenamiento != null && !rutaAlmacenamiento.isBlank()) {
                Path path = Paths.get(rutaAlmacenamiento);
                if (Files.exists(path)) {
                    return Files.readAllBytes(path);
                }
            }
            // Fallback a classpath resource
            ClassPathResource resource = new ClassPathResource("templates/pdf/" + nombreArchivo);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return is.readAllBytes();
                }
            }
            throw new RecursoNoEncontradoException("No se encontró el archivo de plantilla: " + nombreArchivo);
        } catch (IOException e) {
            log.error("Error al leer archivo de plantilla {}", nombreArchivo, e);
            throw new RuntimeException("Error de I/O al leer la plantilla documental", e);
        }
    }

    private String guardarArchivoFisico(String codigo, int version, String sha256, String nombreOriginal, byte[] contenido) {
        try {
            File dir = new File(almacenamientoDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String nombreDestino = codigo.toLowerCase() + "_v" + version + "_" + sha256.substring(0, 8) + ".pdf";
            File archivoDestino = new File(dir, nombreDestino);
            try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                fos.write(contenido);
            }
            return archivoDestino.getAbsolutePath();
        } catch (IOException e) {
            log.error("Error al guardar archivo físico de plantilla en {}", almacenamientoDir, e);
            throw new RuntimeException("No se pudo almacenar físicamente la plantilla", e);
        }
    }

    private PlantillaDocumentalDto mapToDto(PlantillaDocumental p) {
        int totalVersiones = versionRepository.findByPlantillaIdOrderByVersionDesc(p.getId()).size();
        return PlantillaDocumentalDto.builder()
                .id(p.getId())
                .codigo(p.getCodigo())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .baseLegal(p.getBaseLegal())
                .grupo(p.getGrupo())
                .versionActual(p.getVersionActual())
                .versionTexto(p.getVersionTexto())
                .nombreArchivo(p.getNombreArchivo())
                .hashSha256(p.getHashSha256())
                .tamanioBytes(p.getTamanioBytes())
                .fechaActualizacion(p.getFechaActualizacion())
                .actualizadoPor(p.getActualizadoPor())
                .activo(p.getActivo())
                .totalVersionesHistoricas(totalVersiones)
                .build();
    }

    private VersionPlantillaDto mapVersionToDto(PlantillaDocumentalVersion v) {
        return VersionPlantillaDto.builder()
                .id(v.getId())
                .plantillaId(v.getPlantillaId())
                .version(v.getVersion())
                .versionTexto(v.getVersionTexto())
                .nombreArchivo(v.getNombreArchivo())
                .hashSha256(v.getHashSha256())
                .tamanioBytes(v.getTamanioBytes())
                .motivoCambio(v.getMotivoCambio())
                .fechaCreacion(v.getFechaCreacion())
                .creadoPor(v.getCreadoPor())
                .esVersionVigente(v.getEsVersionVigente())
                .build();
    }
}
