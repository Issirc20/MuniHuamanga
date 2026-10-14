package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoBorradorDto {

    private UUID id;
    private UUID borradorId;
    private TipoDocumentoAdjunto tipoDocumento;
    private String nombreArchivo;
    private String tipoContenido;
    private Long tamanioBytes;
    private String hashSha256;
    private LocalDateTime fechaCarga;
    private UUID cargadoPor;
    private String descripcion;
    private String avisoLegal;
}
