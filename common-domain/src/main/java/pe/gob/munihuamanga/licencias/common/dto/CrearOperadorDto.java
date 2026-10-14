package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO para la designación de un nuevo operador nominativo en una cuenta institucional (M01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearOperadorDto {

    @NotBlank(message = "El DNI del operador es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos numéricos")
    private String dniOperador;

    @NotBlank(message = "El nombre completo del operador es obligatorio")
    @Size(min = 3, max = 150)
    private String nombreCompleto;

    @NotBlank(message = "El correo electrónico institucional del operador es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    @NotNull(message = "El rol del operador es obligatorio")
    private RolOperador rolOperador;

    @NotEmpty(message = "Debe asignar al menos un permiso explícito al operador")
    @Builder.Default
    private Set<PermisoOperador> permisos = new HashSet<>();
}
