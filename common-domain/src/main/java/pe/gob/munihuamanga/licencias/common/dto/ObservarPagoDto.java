package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para formular una observación sobre un comprobante de pago declarado (M05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservarPagoDto {

    @NotBlank(message = "El motivo de la observación es obligatorio")
    @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
    private String motivo;
}
