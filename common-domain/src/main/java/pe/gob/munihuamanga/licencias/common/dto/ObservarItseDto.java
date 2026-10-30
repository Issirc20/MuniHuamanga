package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Conjunto de observaciones formuladas a los anexos ITSE cargados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservarItseDto {

    @NotEmpty(message = "Debe registrar al menos una observación.")
    @Valid
    private List<ObservacionItseItemDto> observaciones;
}
