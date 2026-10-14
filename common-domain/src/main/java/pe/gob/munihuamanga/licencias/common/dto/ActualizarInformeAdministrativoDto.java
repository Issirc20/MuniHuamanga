package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarInformeAdministrativoDto {

    @NotNull(message = "La conclusión técnica del informe es obligatoria")
    private ConclusionInforme conclusion;

    @NotBlank(message = "Los fundamentos administrativos son obligatorios")
    @Size(min = 20, max = 3000, message = "Los fundamentos deben tener entre 20 y 3000 caracteres")
    private String fundamentosAdministrativos;

    @NotBlank(message = "Las referencias documentales son obligatorias")
    @Size(min = 10, max = 1500, message = "Las referencias documentales deben tener entre 10 y 1500 caracteres")
    private String referenciasDocumentales;

    @Size(max = 2000, message = "Las observaciones no deben exceder 2000 caracteres")
    private String observaciones;
}
