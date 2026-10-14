package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para el registro de la validación manual de un comprobante por el cajero municipal (M05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarPagoManualDto {

    @NotBlank(message = "El número de operación oficial de caja/banco es obligatorio")
    @Size(max = 60, message = "El número de operación no debe exceder 60 caracteres")
    private String numeroOperacionSat;

    @NotNull(message = "El importe efectivamente validado es obligatorio")
    @DecimalMin(value = "0.01", message = "El importe validado debe ser mayor a cero")
    private BigDecimal montoValidado;

    @NotNull(message = "La fecha de pago real confirmada en caja es obligatoria")
    private LocalDate fechaPagoReal;

    @NotBlank(message = "La justificación y sustento de la validación manual son obligatorios")
    @Size(min = 5, max = 500, message = "La justificación debe tener entre 5 y 500 caracteres")
    private String justificacion;
}
