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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitarRectificacionUbicacionDto {

    @NotNull(message = "El establecimientoId es obligatorio")
    private UUID establecimientoId;

    private UUID expedienteId;

    @NotNull(message = "La latitud propuesta es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    private BigDecimal latitudPropuesta;

    @NotNull(message = "La longitud propuesta es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
    @DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180")
    private BigDecimal longitudPropuesta;

    @NotBlank(message = "La justificación de la rectificación es obligatoria")
    @Size(min = 15, max = 1000, message = "La justificación debe tener entre 15 y 1000 caracteres")
    private String justificacion;

    private UUID documentoSustentoId;
}
