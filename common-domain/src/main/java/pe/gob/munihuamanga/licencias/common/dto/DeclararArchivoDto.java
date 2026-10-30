package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.MotivoArchivo;

/**
 * Declaración de archivo del procedimiento por la autoridad competente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclararArchivoDto {

    @NotNull(message = "Debe indicar la causal de archivo.")
    private MotivoArchivo motivo;

    @NotBlank(message = "El sustento de la declaración es obligatorio.")
    @Size(min = 10, max = 1000, message = "El sustento debe tener entre 10 y 1000 caracteres.")
    private String sustento;
}
