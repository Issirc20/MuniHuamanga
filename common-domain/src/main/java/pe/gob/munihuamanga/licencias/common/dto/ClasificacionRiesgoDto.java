package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasificacionRiesgoDto {

    @NotNull(message = "El nivel de riesgo es obligatorio")
    private NivelRiesgo nivelRiesgo;

    private String informeItseNumero;
    private String observaciones;
    private String usuario;
}
