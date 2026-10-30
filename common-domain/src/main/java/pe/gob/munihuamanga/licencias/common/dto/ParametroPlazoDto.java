package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parámetro de plazo configurable expresado en días hábiles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroPlazoDto {

    private String codigo;

    @NotNull(message = "El valor del plazo es obligatorio.")
    @Min(value = 1, message = "El plazo debe ser de al menos un día hábil.")
    @Max(value = 365, message = "El plazo no debe exceder los 365 días hábiles.")
    private Integer dias;

    private String descripcion;
    private String baseLegal;
}
