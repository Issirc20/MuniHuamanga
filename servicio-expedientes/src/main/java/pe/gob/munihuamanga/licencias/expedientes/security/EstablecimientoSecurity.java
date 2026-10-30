package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;

import java.util.UUID;

/**
 * Política de acceso a los establecimientos registrados por las cuentas de administrado.
 */
@Component("establecimientoSecurity")
@RequiredArgsConstructor
public class EstablecimientoSecurity {

    private final EstablecimientoRepository establecimientoRepository;
    private final ProveedorContextoSeguridad proveedorContexto;
    private final ServicioRepresentacion servicioRepresentacion;

    public boolean puedeVerEstablecimientosDeCuenta(Authentication authentication, UUID cuentaId) {
        if (cuentaId == null || !ProveedorContextoSeguridad.estaAutenticado(authentication)) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        if (actor.tieneAlgunRol(RolesInstitucionales.CONSULTA_EXPEDIENTES) || actor.tieneRol(RolUsuario.ROLE_INSPECTOR)) {
            return true;
        }
        return servicioRepresentacion.tieneFacultad(actor.usuarioId(), cuentaId,
                PermisoOperador.CONSULTAR_EXPEDIENTE, PermisoOperador.GESTIONAR_ESTABLECIMIENTOS);
    }

    public boolean puedeGestionarEstablecimientosDeCuenta(Authentication authentication, UUID cuentaId) {
        if (cuentaId == null || !ProveedorContextoSeguridad.estaAutenticado(authentication)) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        if (actor.tieneAlgunRol(RolesInstitucionales.ATENCION_PRESENCIAL)) {
            return true;
        }
        return servicioRepresentacion.tieneFacultad(actor.usuarioId(), cuentaId, PermisoOperador.GESTIONAR_ESTABLECIMIENTOS);
    }

    public boolean puedeVerEstablecimientoPorId(Authentication authentication, UUID establecimientoId) {
        return establecimientoRepository.findById(establecimientoId)
                .map(est -> puedeVerEstablecimientosDeCuenta(authentication, est.getCuenta().getId()))
                .orElse(false);
    }

    public boolean puedeModificarEstablecimientoPorId(Authentication authentication, UUID establecimientoId) {
        return establecimientoRepository.findById(establecimientoId)
                .map(est -> puedeGestionarEstablecimientosDeCuenta(authentication, est.getCuenta().getId()))
                .orElse(false);
    }
}
