package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para el registro de un nuevo establecimiento físico vinculado a una Cuenta (M02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearEstablecimientoDto {

    @NotBlank(message = "El nombre comercial del establecimiento es obligatorio")
    @Size(min = 2, max = 150)
    private String nombreComercial;

    @NotBlank(message = "La dirección física del establecimiento es obligatoria")
    @Size(min = 5, max = 255)
    private String direccion;

    @Builder.Default
    private String distrito = "Ayacucho";

    @Builder.Default
    private String provincia = "Huamanga";

    @Builder.Default
    private String departamento = "Ayacucho";

    private String referencia;

    @NotNull(message = "La latitud geográfica es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud mínima permitida es -90.0")
    @DecimalMax(value = "90.0", message = "La latitud máxima permitida es 90.0")
    private BigDecimal latitud;

    @NotNull(message = "La longitud geográfica es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud mínima permitida es -180.0")
    @DecimalMax(value = "180.0", message = "La longitud máxima permitida es 180.0")
    private BigDecimal longitud;

    @NotBlank(message = "El código CIIU de actividad económica es obligatorio")
    private String ciiuCodigo;

    @NotBlank(message = "El giro principal es obligatorio")
    private String giroPrincipal;

    @NotNull(message = "El área en metros cuadrados es obligatoria")
    @DecimalMin(value = "1.0", message = "El área debe ser al menos de 1.0 m²")
    private BigDecimal areaMetrosCuadrados;
}
