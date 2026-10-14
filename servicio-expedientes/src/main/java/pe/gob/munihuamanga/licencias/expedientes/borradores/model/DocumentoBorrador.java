package pe.gob.munihuamanga.licencias.expedientes.borradores.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documentos_borrador")
public class DocumentoBorrador {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrador_id", nullable = false)
    private SolicitudBorrador borrador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 50)
    private TipoDocumentoAdjunto tipoDocumento;

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

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
}
