package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

/**
 * Registro de la carga de los cuatro anexos ITSE firmados.
 * El nivel declarado es el que figura en los anexos y sirve para la liquidación referencial;
 * el nivel oficial lo confirma la oficina de licencias al validar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarCargaItseDto {

    @NotNull(message = "Debe indicar el nivel de riesgo consignado en los anexos firmados.")
    private NivelRiesgo nivelDeclarado;

    @Size(max = 500, message = "El comentario no debe exceder los 500 caracteres.")
    private String comentario;
}
