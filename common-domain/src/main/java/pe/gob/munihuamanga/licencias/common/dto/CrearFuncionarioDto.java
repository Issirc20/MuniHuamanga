package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;

import java.util.Set;

/**
 * Alta de un funcionario municipal. La contraseña la define el propio funcionario
 * mediante el enlace de activación que recibe por correo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearFuncionarioDto {

    @NotBlank(message = "El nombre de usuario es obligatorio.")
    @Pattern(regexp = "^[a-z0-9._-]{3,50}$", message = "El usuario solo admite minúsculas, números, punto, guion y guion bajo (3 a 50 caracteres).")
    private String username;

    @NotBlank(message = "El nombre completo es obligatorio.")
    @Size(max = 150, message = "El nombre no debe exceder los 150 caracteres.")
    private String nombreCompleto;

    @NotBlank(message = "El correo institucional es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido.")
    private String email;

    @NotEmpty(message = "Debe asignar al menos un rol institucional.")
    private Set<RolUsuario> roles;
}
