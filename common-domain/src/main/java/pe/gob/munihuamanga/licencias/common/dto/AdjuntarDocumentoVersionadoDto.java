package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;

import java.util.UUID;

/**
 * DTO para la carga de una nueva versión documental (Incremento I05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjuntarDocumentoVersionadoDto {

    @NotNull(message = "El tipo de documento es obligatorio.")
    private TipoDocumentoAdjunto tipoDocumento;

    @NotBlank(message = "El nombre de archivo es obligatorio.")
    private String nombreArchivo;

    private String tipoContenido;

    @NotBlank(message = "El contenido en Base64 es obligatorio.")
    private String contenidoBase64;

    private String motivoActualizacion;

    private UUID observacionId;
}
