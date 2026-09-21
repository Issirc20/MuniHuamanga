package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolucionExpedienteDto {

    private boolean aprobado;

    @NotBlank(message = "El motivo u observaciones de la resolución son obligatorios")
    private String motivo;

    private String funcionarioResponsable;
}
