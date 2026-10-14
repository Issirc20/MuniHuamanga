package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SolicitudBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;

import java.util.UUID;

@Slf4j
@Component("borradorSecurity")
@RequiredArgsConstructor
public class BorradorSecurity {

    private final SolicitudBorradorRepository borradorRepository;
    private final CuentaRepository cuentaRepository;
    private final OperadorCuentaRepository operadorCuentaRepository;

    public boolean puedeCrearBorradorEnCuenta(Authentication authentication, UUID cuentaId) {
        if (!estaAutenticado(authentication) || cuentaId == null) return false;
        if (tieneRolFuncionario(authentication)) return true;

        Cuenta cuenta = cuentaRepository.findById(cuentaId).orElse(null);
        if (cuenta == null) return false;

        // 1. Titular directo (por username o ID de cuenta)
        if (esTitularCuenta(authentication, cuenta)) return true;

        // 2. Operador con permiso CREAR_BORRADOR o CREAR_EXPEDIENTE
        String username = authentication.getName();
        return esOperadorConPermiso(cuentaId, username, PermisoOperador.CREAR_BORRADOR, PermisoOperador.CREAR_EXPEDIENTE);
    }

    public boolean puedeVerBorrador(Authentication authentication, UUID borradorId) {
        if (!estaAutenticado(authentication) || borradorId == null) return false;
        if (tieneRolFuncionario(authentication)) return true;

        SolicitudBorrador borrador = borradorRepository.findById(borradorId).orElse(null);
        if (borrador == null) return false;

        Cuenta cuenta = borrador.getCuenta();
        if (esTitularCuenta(authentication, cuenta)) return true;

        String username = authentication.getName();
        return esOperadorConPermiso(cuenta.getId(), username,
                PermisoOperador.CONSULTAR_EXPEDIENTE,
                PermisoOperador.CREAR_BORRADOR,
                PermisoOperador.CREAR_EXPEDIENTE);
    }

    public boolean puedeEditarBorrador(Authentication authentication, UUID borradorId) {
        if (!estaAutenticado(authentication) || borradorId == null) return false;

        SolicitudBorrador borrador = borradorRepository.findById(borradorId).orElse(null);
        if (borrador == null) return false;

        Cuenta cuenta = borrador.getCuenta();
        if (esTitularCuenta(authentication, cuenta)) return true;

        String username = authentication.getName();
        return esOperadorConPermiso(cuenta.getId(), username,
                PermisoOperador.CREAR_BORRADOR,
                PermisoOperador.CREAR_EXPEDIENTE);
    }

    public boolean puedePresentarBorrador(Authentication authentication, UUID borradorId) {
        if (!estaAutenticado(authentication) || borradorId == null) return false;

        SolicitudBorrador borrador = borradorRepository.findById(borradorId).orElse(null);
        if (borrador == null) return false;

        Cuenta cuenta = borrador.getCuenta();
        if (esTitularCuenta(authentication, cuenta)) return true;

        String username = authentication.getName();
        return esOperadorConPermiso(cuenta.getId(), username,
                PermisoOperador.PRESENTAR_EXPEDIENTE,
                PermisoOperador.CREAR_EXPEDIENTE);
    }

    private boolean esTitularCuenta(Authentication authentication, Cuenta cuenta) {
        String username = authentication.getName();
        if (username != null && username.equalsIgnoreCase(cuenta.getNumeroDocumento())) return true;
        if (authentication.getPrincipal() instanceof CustomUserDetails cud && cud.getId().equals(cuenta.getId())) return true;
        return false;
    }

    private boolean esOperadorConPermiso(UUID cuentaId, String username, PermisoOperador... permisosRequeridos) {
        return operadorCuentaRepository.findByCuentaId(cuentaId).stream()
                .filter(op -> Boolean.TRUE.equals(op.getActivo()))
                .filter(op -> op.getDniOperador().equalsIgnoreCase(username) ||
                        (op.getUsuario() != null && op.getUsuario().getUsername().equalsIgnoreCase(username)))
                .anyMatch(op -> {
                    for (PermisoOperador req : permisosRequeridos) {
                        if (op.getPermisos().contains(req)) return true;
                    }
                    return false;
                });
    }

    private boolean estaAutenticado(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }

    private boolean tieneRolFuncionario(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals(RolUsuario.ROLE_ADMIN.name()) ||
                        a.getAuthority().equals(RolUsuario.ROLE_EVALUADOR.name()));
    }
}
