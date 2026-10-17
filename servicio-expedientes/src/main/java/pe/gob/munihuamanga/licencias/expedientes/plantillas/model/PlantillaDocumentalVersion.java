package pe.gob.munihuamanga.licencias.expedientes.plantillas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que registra el historial inmutable de versiones de una plantilla oficial.
 */
@Entity
@Table(name = "plantillas_documentales_versiones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaDocumentalVersion {

    @Id
    private UUID id;

    @Column(name = "plantilla_id", nullable = false)
    private UUID plantillaId;

    @Column(name = "codigo_plantilla", nullable = false, length = 50)
    private String codigoPlantilla;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "version_texto", nullable = false, length = 30)
    private String versionTexto;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "ruta_almacenamiento", columnDefinition = "TEXT")
    private String rutaAlmacenamiento;

    @Column(name = "motivo_cambio", columnDefinition = "TEXT")
    private String motivoCambio;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "creado_por", length = 100)
    private String creadoPor;

    @Column(name = "es_version_vigente", nullable = false)
    private Boolean esVersionVigente;
}
