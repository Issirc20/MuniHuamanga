package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Activación de un acceso mediante un enlace de un solo uso (invitación de operador o alta de funcionario).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstablecerContrasenaDto {

    @NotBlank(message = "El código de activación es obligatorio.")
    private String token;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(max = 100, message = "La contraseña no debe exceder los 100 caracteres.")
    private String password;
}
