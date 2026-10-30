package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;

import java.util.UUID;

/**
 * Política de acceso a cuentas de administrado.
 */
@Component("cuentaSecurity")
@RequiredArgsConstructor
public class CuentaSecurity {

    private final ProveedorContextoSeguridad proveedorContexto;
    private final ServicioRepresentacion servicioRepresentacion;

    public boolean puedeVerCuenta(Authentication authentication, UUID cuentaId) {
        if (!ProveedorContextoSeguridad.estaAutenticado(authentication)) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        if (actor.tieneAlgunRol(RolesInstitucionales.CONSULTA_EXPEDIENTES) || actor.tieneRol(RolUsuario.ROLE_INSPECTOR)) {
            return true;
        }
        return servicioRepresentacion.tieneFacultad(actor.usuarioId(), cuentaId);
    }

    public boolean puedeGestionarOperadores(Authentication authentication, UUID cuentaId) {
        if (!ProveedorContextoSeguridad.estaAutenticado(authentication)) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        return servicioRepresentacion.tieneFacultad(actor.usuarioId(), cuentaId, PermisoOperador.ADMINISTRAR_OPERADORES);
    }
}
