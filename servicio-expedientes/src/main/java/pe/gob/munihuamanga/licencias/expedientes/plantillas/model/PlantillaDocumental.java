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
 * Entidad que representa una plantilla documental oficial vigente del sistema.
 * Asegura versionado inmutable, no retroactividad y trazabilidad SHA-256.
 */
@Entity
@Table(name = "plantillas_documentales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaDocumental {

    @Id
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo; // ITSE_ANEXO_1, ITSE_ANEXO_2, ITSE_ANEXO_3, ITSE_ANEXO_4, LICENCIA_ANEXO_1

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "base_legal", length = 255)
    private String baseLegal;

    @Column(name = "grupo", nullable = false, length = 30)
    private String grupo; // ITSE o LICENCIA

    @Column(name = "version_actual", nullable = false)
    private Integer versionActual;

    @Column(name = "version_texto", nullable = false, length = 30)
    private String versionTexto; // ej: "v1.0 (2026)"

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "ruta_almacenamiento", columnDefinition = "TEXT")
    private String rutaAlmacenamiento;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "actualizado_por", length = 100)
    private String actualizadoPor;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
