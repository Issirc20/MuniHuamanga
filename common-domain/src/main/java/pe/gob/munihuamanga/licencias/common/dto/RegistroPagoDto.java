package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPagoDto {

    @NotBlank(message = "El identificador del voucher es obligatorio")
    private String voucherId;

    @NotBlank(message = "El número de operación bancaria o SAT es obligatorio")
    private String numeroOperacionSat;

    @NotNull(message = "El monto pagado es obligatorio")
    private BigDecimal montoPagado;

    private String usuario;
}
