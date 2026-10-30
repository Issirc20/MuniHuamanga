package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de reenvío del enlace de verificación de correo electrónico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReenviarVerificacionDto {

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido.")
    private String email;
}
