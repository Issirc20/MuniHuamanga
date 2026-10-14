package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO para la modificación controlada y auditada de permisos y roles de un operador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPermisosOperadorDto {

    private RolOperador rolOperador;

    @NotEmpty(message = "Debe asignar al menos un permiso explícito al operador")
    @Builder.Default
    private Set<PermisoOperador> permisos = new HashSet<>();

    private Boolean activo;
}
