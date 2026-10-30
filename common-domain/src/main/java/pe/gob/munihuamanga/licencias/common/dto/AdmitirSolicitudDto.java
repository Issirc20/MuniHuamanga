package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.MedioValidacionIdentidad;

/**
 * Decisión de admisibilidad de una solicitud presentada.
 * Si la identidad del administrado aún no ha sido validada, mesa de partes debe indicar
 * el medio por el cual la verificó en este acto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmitirSolicitudDto {

    @NotBlank(message = "El sustento de la admisión es obligatorio.")
    @Size(min = 10, max = 1000, message = "El sustento debe tener entre 10 y 1000 caracteres.")
    private String sustento;

    private MedioValidacionIdentidad medioValidacionIdentidad;

    @Size(max = 200, message = "La referencia de la validación no debe exceder los 200 caracteres.")
    private String referenciaValidacionIdentidad;
}
