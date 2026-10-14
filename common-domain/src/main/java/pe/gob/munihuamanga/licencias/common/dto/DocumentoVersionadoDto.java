package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para representación y consulta de documentos versionados (Incremento I05).
 * Nunca expone rutas internas de almacenamiento físico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoVersionadoDto {

    private UUID id;
    private UUID expedienteId;
    private UUID borradorId;
    private TipoDocumentoAdjunto tipoDocumento;
    private Integer version;
    private String nombreArchivo;
    private String tipoContenido;
    private Long tamanioBytes;
    private String hashSha256;
    private LocalDateTime fechaCarga;
    private UUID cargadoPor;
    private String cargadoPorNombre;
    private EstadoRevisionDocumento estadoRevision;
    private UUID versionAnteriorId;
    private String motivoActualizacion;
    private Boolean esVersionActual;
    private UUID observacionId;
    private String contenidoBase64;
}
