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
public class EvaluarRectificacionUbicacionDto {

    private boolean aprobado;

    @NotBlank(message = "El motivo de la respuesta de evaluación es obligatorio")
    @Size(min = 15, max = 1500, message = "El motivo debe tener entre 15 y 1500 caracteres")
    private String motivoRespuesta;

    private boolean esTrasladoFisico;
}
