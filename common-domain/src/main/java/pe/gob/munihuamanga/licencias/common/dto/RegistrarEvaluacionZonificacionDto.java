package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;

import java.util.UUID;

/**
 * DTO para el registro administrativo de la evaluación de zonificación y compatibilidad de uso (M07).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEvaluacionZonificacionDto {

    private UUID establecimientoId;

    private UUID documentoSustentatorioId;

    @NotNull(message = "El resultado de la evaluación es obligatorio")
    private ResultadoZonificacion resultado;

    @NotBlank(message = "La referencia de zonificación o PDU aplicable es obligatoria")
    @Size(min = 5, max = 255, message = "La referencia de zonificación debe tener entre 5 y 255 caracteres")
    private String referenciaZonificacion;

    @NotBlank(message = "La fuente del informe o sustento técnico oficial es obligatoria")
    @Size(min = 5, max = 255, message = "La fuente de sustento técnico debe tener entre 5 y 255 caracteres")
    private String fuenteSustentoTecnico;

    @NotBlank(message = "Las observaciones y motivación del dictamen son obligatorias")
    @Size(min = 5, max = 1000, message = "Las observaciones deben tener entre 5 y 1000 caracteres")
    private String observaciones;
}
