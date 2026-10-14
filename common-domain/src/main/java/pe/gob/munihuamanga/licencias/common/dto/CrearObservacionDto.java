package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import pe.gob.munihuamanga.licencias.common.enums.TipoObservacion;

import java.util.UUID;

/**
 * DTO para formular una observación técnica o documental sobre un expediente (Ley N° 27444 / TUPA MPH).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearObservacionDto {

    private UUID documentoId;

    @NotBlank(message = "Debe indicar el requisito o documento afectado por la observación.")
    private String requisitoAfectado;

    @NotBlank(message = "El motivo de la observación es obligatorio y debe estar fundamentado.")
    private String motivo;

    private String fundamentoLegal;

    private TipoObservacion tipoObservacion;

    /**
     * Plazo en días hábiles configurado según el procedimiento (Ley 27444 / D.S. 002-2018-PCM / TUPA).
     */
    private Integer plazoDiasHabiles;
}
