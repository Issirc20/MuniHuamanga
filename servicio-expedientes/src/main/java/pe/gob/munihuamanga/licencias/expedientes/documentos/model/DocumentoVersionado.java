package pe.gob.munihuamanga.licencias.expedientes.documentos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de documento versionado inmutable (Incremento I05).
 * Asegura la conservación de versiones históricas, hashes SHA-256 y trazabilidad de cambios.
 */
@Entity
@Table(
        name = "documentos_versionados",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_doc_expediente_version", columnNames = {"expediente_id", "tipo_documento", "version"}),
                @UniqueConstraint(name = "uk_doc_borrador_version", columnNames = {"borrador_id", "tipo_documento", "version"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoVersionado {

    @Id
    private UUID id;

    @Column(name = "expediente_id")
    private UUID expedienteId;

    @Column(name = "borrador_id")
    private UUID borradorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 50)
    private TipoDocumentoAdjunto tipoDocumento;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", length = 100)
    private String tipoContenido;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "ruta_almacenamiento", columnDefinition = "TEXT")
    private String rutaAlmacenamiento;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "cargado_por", nullable = false)
    private UUID cargadoPor;

    @Column(name = "cargado_por_nombre", length = 100)
    private String cargadoPorNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_revision", nullable = false, length = 30)
    private EstadoRevisionDocumento estadoRevision;

    @Column(name = "version_anterior_id")
    private UUID versionAnteriorId;

    @Column(name = "motivo_actualizacion", columnDefinition = "TEXT")
    private String motivoActualizacion;

    @Column(name = "es_version_actual", nullable = false)
    private Boolean esVersionActual;

    @Column(name = "observacion_id")
    private UUID observacionId;
}
