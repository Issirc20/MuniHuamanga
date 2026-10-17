package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la carga y sustitución controlada de una plantilla documental por una nueva versión.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SustituirPlantillaDto {

    @NotBlank(message = "El nombre del archivo es obligatorio")
    private String nombreArchivo;

    @NotBlank(message = "El contenido del archivo en base64 es obligatorio")
    private String contenidoBase64;

    @NotBlank(message = "El motivo de actualización o sustitución es obligatorio para la auditoría")
    private String motivoCambio;
}
