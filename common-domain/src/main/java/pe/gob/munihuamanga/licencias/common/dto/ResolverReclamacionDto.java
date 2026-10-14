package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolverReclamacionDto {

    @NotNull(message = "Debe indicar si aprueba o rechaza la reclamación")
    private Boolean aprobado;

    @NotBlank(message = "El motivo o sustento de la resolución es obligatorio")
    private String motivoResolucion;
}
