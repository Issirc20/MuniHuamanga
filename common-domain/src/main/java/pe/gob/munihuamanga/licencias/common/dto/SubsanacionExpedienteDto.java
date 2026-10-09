package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el registro formal de subsanación de observaciones (Ley N° 27444 LPAG).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubsanacionExpedienteDto {

    @NotBlank(message = "El detalle de la subsanación de observaciones es obligatorio")
    private String detalleSubsanacion;

    private String usuario;
}
