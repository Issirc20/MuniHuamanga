package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para la actualización de datos de un establecimiento comercial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarEstablecimientoDto {

    @Size(min = 2, max = 150)
    private String nombreComercial;

    @Size(min = 5, max = 255)
    private String direccion;

    private String distrito;
    private String referencia;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitud;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitud;

    private String ciiuCodigo;
    private String giroPrincipal;

    @DecimalMin(value = "1.0")
    private BigDecimal areaMetrosCuadrados;

    private Boolean activo;
}
