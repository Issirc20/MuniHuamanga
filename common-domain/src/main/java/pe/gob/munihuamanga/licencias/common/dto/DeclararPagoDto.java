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
import java.util.UUID;

/**
 * DTO para la declaración ciudadana de un comprobante de pago de tasa administrativa (M05).
 * El registro coloca el pago en PENDIENTE_VALIDACION sin confirmar el pago oficialmente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclararPagoDto {

    @NotBlank(message = "El código de comprobante o número de operación es obligatorio")
    @Size(min = 3, max = 60, message = "El código de comprobante debe tener entre 3 y 60 caracteres")
    private String codigoComprobante;

    @NotNull(message = "La fecha declarada de pago es obligatoria")
    private LocalDate fechaDeclarada;

    @NotNull(message = "El importe declarado es obligatorio")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor a cero")
    private BigDecimal montoDeclarado;

    private UUID documentoSustentatorioId;

    @Size(max = 500, message = "Las observaciones no deben exceder 500 caracteres")
    private String observacionesAdministrado;
}
