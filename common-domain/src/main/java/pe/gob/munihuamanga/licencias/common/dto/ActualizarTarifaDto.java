package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para la actualización de tarifas TUPA por parte del Administrador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarTarifaDto {

    @NotNull(message = "El monto total de la tasa es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal montoTotal;

    @DecimalMin(value = "0.00", message = "El derecho de trámite no puede ser negativo")
    private BigDecimal derechoTramite;

    @DecimalMin(value = "0.00", message = "El costo de ITSE no puede ser negativo")
    private BigDecimal costoItse;

    private String concepto;

    private String baseLegal;

    private Boolean activo;
}
