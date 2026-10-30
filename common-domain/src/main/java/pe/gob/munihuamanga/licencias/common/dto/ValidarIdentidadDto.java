package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.MedioValidacionIdentidad;

/**
 * Validación de la identidad declarada de un administrado por mesa de partes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarIdentidadDto {

    @NotNull(message = "Debe indicar el medio de validación de identidad.")
    private MedioValidacionIdentidad medio;

    @NotBlank(message = "Debe registrar una referencia de la validación efectuada.")
    @Size(max = 200, message = "La referencia no debe exceder los 200 caracteres.")
    private String referencia;
}
