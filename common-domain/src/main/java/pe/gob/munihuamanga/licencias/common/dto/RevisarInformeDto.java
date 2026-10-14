package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisarInformeDto {

    @NotBlank(message = "La acción de revisión es obligatoria (VISAR o DEVOLVER)")
    private String accion;

    @NotBlank(message = "El sustento de la revisión u observación es obligatorio")
    @Size(min = 10, max = 2000, message = "El sustento debe tener entre 10 y 2000 caracteres")
    private String observacionesRevision;
}
