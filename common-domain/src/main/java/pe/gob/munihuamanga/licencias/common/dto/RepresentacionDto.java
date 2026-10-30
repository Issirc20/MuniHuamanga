package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

import java.util.Set;
import java.util.UUID;

/**
 * Cuenta de administrado en cuyo nombre puede actuar la persona autenticada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepresentacionDto {

    private UUID representacionId;
    private UUID cuentaId;
    private TipoPersona tipoPersona;
    private String numeroDocumentoCuenta;
    private String razonSocialONombre;
    private RolOperador rol;
    private Set<PermisoOperador> permisos;
    private EstadoCuenta estadoCuenta;
}
