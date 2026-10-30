package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sustento escrito de una decisión adoptada por un funcionario sobre el expediente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SustentoDto {

    @NotBlank(message = "El sustento de la decisión es obligatorio.")
    @Size(min = 10, max = 1000, message = "El sustento debe tener entre 10 y 1000 caracteres.")
    private String sustento;
}
