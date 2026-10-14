package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjuntarDocumentoBorradorDto {

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumentoAdjunto tipoDocumento;

    @NotBlank(message = "El nombre de archivo es obligatorio")
    private String nombreArchivo;

    private String tipoContenido;

    @NotBlank(message = "El contenido Base64 es obligatorio")
    private String contenidoBase64;

    private String descripcion;
}
