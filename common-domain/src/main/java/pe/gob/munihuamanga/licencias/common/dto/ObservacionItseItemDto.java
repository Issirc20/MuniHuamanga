package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.common.enums.TipoObservacionItse;

/**
 * Observación sobre un anexo ITSE específico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacionItseItemDto {

    @NotNull(message = "Debe indicar el anexo observado.")
    private TipoAnexoItse anexo;

    @NotNull(message = "Debe indicar el tipo de observación.")
    private TipoObservacionItse tipo;

    @NotBlank(message = "El motivo de la observación es obligatorio.")
    @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres.")
    private String motivo;
}
