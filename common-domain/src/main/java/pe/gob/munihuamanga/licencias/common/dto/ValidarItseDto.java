package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

/**
 * Resultado de la verificación formal de los anexos ITSE por la oficina de licencias.
 * Cada casilla corresponde a un punto de la lista de verificación y debe declararse conforme.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarItseDto {

    @NotNull(message = "Debe confirmar el nivel de riesgo oficial consignado por Defensa Civil.")
    private NivelRiesgo nivelOficial;

    private boolean anexosCompletosYLegibles;
    private boolean firmasYSellosConformes;
    private boolean datosCoincidenConSolicitud;
    private boolean nivelRiesgoCoherente;

    @Size(max = 500, message = "El comentario no debe exceder los 500 caracteres.")
    private String comentario;
}
