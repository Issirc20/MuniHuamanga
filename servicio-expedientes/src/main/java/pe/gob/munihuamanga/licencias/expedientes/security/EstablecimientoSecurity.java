package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Componente de Evaluación de Autorización a Nivel de Objeto para Establecimientos (M02).
 * Restringe la consulta y modificación al titular u operador nominado con permisos explícitos.
 */
@Slf4j
@Component("establecimientoSecurity")
@RequiredArgsConstructor
public class EstablecimientoSecurity {

    private final CuentaRepository cuentaRepository;
    private final EstablecimientoRepository establecimientoRepository;
    private final OperadorCuentaRepository operadorCuentaRepository;

    public boolean puedeVerEstablecimientosDeCuenta(Authentication authentication, UUID cuentaId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (tieneRol(authentication, "ROLE_ADMIN", "ROLE_EVALUADOR", "ROLE_INSPECTOR")) {
            return true;
        }

        Optional<Cuenta> optCuenta = cuentaRepository.findById(cuentaId);
        if (optCuenta.isEmpty()) return false;
        Cuenta cuenta = optCuenta.get();

        String username = authentication.getName();

        if (username.equalsIgnoreCase(cuenta.getNumeroDocumento()) ||
                (cuenta.getEmail() != null && username.equalsIgnoreCase(cuenta.getEmail()))) {
            return true;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails cud && cud.getId().equals(cuenta.getId())) {
            return true;
        }

        return operadorCuentaRepository.findByCuentaId(cuentaId).stream()
                .anyMatch(op -> op.getActivo() &&
                        (op.getDniOperador().equalsIgnoreCase(username) ||
                                (op.getUsuario() != null && op.getUsuario().getUsername().equalsIgnoreCase(username))) &&
                        (op.getPermisos().contains(PermisoOperador.CONSULTAR_EXPEDIENTE) ||
                                op.getPermisos().contains(PermisoOperador.GESTIONAR_ESTABLECIMIENTOS)));
    }

    public boolean puedeGestionarEstablecimientosDeCuenta(Authentication authentication, UUID cuentaId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (tieneRol(authentication, "ROLE_ADMIN")) {
            return true;
        }

        Optional<Cuenta> optCuenta = cuentaRepository.findById(cuentaId);
        if (optCuenta.isEmpty()) return false;
        Cuenta cuenta = optCuenta.get();

        String username = authentication.getName();

        if (username.equalsIgnoreCase(cuenta.getNumeroDocumento()) ||
                (cuenta.getEmail() != null && username.equalsIgnoreCase(cuenta.getEmail()))) {
            return true;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails cud && cud.getId().equals(cuenta.getId())) {
            return true;
        }

        return operadorCuentaRepository.findByCuentaId(cuentaId).stream()
                .anyMatch(op -> op.getActivo() &&
                        (op.getDniOperador().equalsIgnoreCase(username) ||
                                (op.getUsuario() != null && op.getUsuario().getUsername().equalsIgnoreCase(username))) &&
                        op.getPermisos().contains(PermisoOperador.GESTIONAR_ESTABLECIMIENTOS));
    }

    public boolean puedeVerEstablecimientoPorId(Authentication authentication, UUID establecimientoId) {
        Optional<Establecimiento> optEst = establecimientoRepository.findById(establecimientoId);
        return optEst.filter(est -> puedeVerEstablecimientosDeCuenta(authentication, est.getCuenta().getId())).isPresent();
    }

    public boolean puedeModificarEstablecimientoPorId(Authentication authentication, UUID establecimientoId) {
        Optional<Establecimiento> optEst = establecimientoRepository.findById(establecimientoId);
        return optEst.filter(est -> puedeGestionarEstablecimientosDeCuenta(authentication, est.getCuenta().getId())).isPresent();
    }

    private boolean tieneRol(Authentication auth, String... roles) {
        for (String rol : roles) {
            boolean coincide = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equalsIgnoreCase(rol));
            if (coincide) return true;
        }
        return false;
    }
}
