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
public class AprobarInformeDto {

    @NotBlank(message = "El sustento o justificación de aprobación es obligatorio")
    @Size(min = 10, max = 2000, message = "El sustento de aprobación debe tener entre 10 y 2000 caracteres")
    private String observacionesAprobacion;
}
