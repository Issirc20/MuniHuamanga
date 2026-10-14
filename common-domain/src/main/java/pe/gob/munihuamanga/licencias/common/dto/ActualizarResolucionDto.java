package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarResolucionDto {

    private TipoDecisionResolucion tipoDecision;

    @Size(min = 20, max = 5000, message = "Los fundamentos deben contener entre 20 y 5000 caracteres")
    private String fundamentos;

    @Size(max = 2000, message = "Las referencias legales no deben superar los 2000 caracteres")
    private String referenciasLegales;

    @Size(max = 3000, message = "La parte resolutiva no debe superar los 3000 caracteres")
    private String parteResolutiva;
}
