package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SolicitudBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;

import java.util.UUID;

/**
 * Política de acceso a solicitudes en borrador.
 * El borrador pertenece a la cuenta del administrado; mesa de partes puede elaborarlo
 * y presentarlo en su nombre cuando la solicitud se recibe en ventanilla.
 */
@Component("borradorSecurity")
@RequiredArgsConstructor
public class BorradorSecurity {

    private final SolicitudBorradorRepository borradorRepository;
    private final ProveedorContextoSeguridad proveedorContexto;
    private final ServicioRepresentacion servicioRepresentacion;

    public boolean puedeCrearBorradorEnCuenta(Authentication authentication, UUID cuentaId) {
        return autorizado(authentication, cuentaId, true,
                PermisoOperador.CREAR_BORRADOR, PermisoOperador.CREAR_EXPEDIENTE);
    }

    public boolean puedeVerBorrador(Authentication authentication, UUID borradorId) {
        UUID cuentaId = cuentaDelBorrador(borradorId);
        if (cuentaId == null || !ProveedorContextoSeguridad.estaAutenticado(authentication)) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        if (actor.tieneAlgunRol(RolesInstitucionales.CONSULTA_EXPEDIENTES)) {
            return true;
        }
        return servicioRepresentacion.tieneFacultad(actor.usuarioId(), cuentaId,
                PermisoOperador.CONSULTAR_EXPEDIENTE, PermisoOperador.CREAR_BORRADOR, PermisoOperador.CREAR_EXPEDIENTE);
    }

    public boolean puedeEditarBorrador(Authentication authentication, UUID borradorId) {
        return autorizado(authentication, cuentaDelBorrador(borradorId), true,
                PermisoOperador.CREAR_BORRADOR, PermisoOperador.CREAR_EXPEDIENTE);
    }

    public boolean puedePresentarBorrador(Authentication authentication, UUID borradorId) {
        return autorizado(authentication, cuentaDelBorrador(borradorId), true,
                PermisoOperador.PRESENTAR_EXPEDIENTE, PermisoOperador.CREAR_EXPEDIENTE);
    }

    private boolean autorizado(Authentication authentication, UUID cuentaId, boolean admiteVentanilla, PermisoOperador... permisos) {
        if (cuentaId == null || !ProveedorContextoSeguridad.estaAutenticado(authentication)) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        if (admiteVentanilla && actor.tieneAlgunRol(RolesInstitucionales.ATENCION_PRESENCIAL)) {
            return true;
        }
        return servicioRepresentacion.tieneFacultad(actor.usuarioId(), cuentaId, permisos);
    }

    private UUID cuentaDelBorrador(UUID borradorId) {
        if (borradorId == null) {
            return null;
        }
        return borradorRepository.findById(borradorId)
                .map(SolicitudBorrador::getCuenta)
                .map(cuenta -> cuenta.getId())
                .orElse(null);
    }
}
