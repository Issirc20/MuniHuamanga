package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;

import java.util.Set;

/**
 * Actualización de los roles institucionales de un funcionario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarRolesDto {

    @NotEmpty(message = "Debe asignar al menos un rol institucional.")
    private Set<RolUsuario> roles;
}
