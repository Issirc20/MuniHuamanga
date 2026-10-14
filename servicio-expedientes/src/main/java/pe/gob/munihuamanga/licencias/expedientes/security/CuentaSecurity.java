package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Componente de Evaluación de Autorización a Nivel de Objeto para Cuentas (M01).
 * Evita elevación de privilegios y accesos no autorizados a cuentas ajenas (OWASP API1:2023).
 */
@Slf4j
@Component("cuentaSecurity")
@RequiredArgsConstructor
public class CuentaSecurity {

    private final CuentaRepository cuentaRepository;
    private final OperadorCuentaRepository operadorCuentaRepository;

    public boolean puedeVerCuenta(Authentication authentication, UUID cuentaId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 1. Roles administrativos y de fiscalización
        if (tieneRol(authentication, "ROLE_ADMIN", "ROLE_EVALUADOR", "ROLE_INSPECTOR")) {
            return true;
        }

        Optional<Cuenta> optCuenta = cuentaRepository.findById(cuentaId);
        if (optCuenta.isEmpty()) {
            return false;
        }
        Cuenta cuenta = optCuenta.get();

        String username = authentication.getName();

        // 2. Titular directo (por username o ID)
        if (username.equalsIgnoreCase(cuenta.getNumeroDocumento())) {
            return true;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails cud && cud.getId().equals(cuenta.getId())) {
            return true;
        }

        // 3. Operador activo de la cuenta
        return operadorCuentaRepository.findByCuentaId(cuentaId).stream()
                .anyMatch(op -> op.getActivo() && (op.getDniOperador().equalsIgnoreCase(username) ||
                        (op.getUsuario() != null && op.getUsuario().getUsername().equalsIgnoreCase(username))));
    }

    public boolean puedeGestionarOperadores(Authentication authentication, UUID cuentaId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (tieneRol(authentication, "ROLE_ADMIN")) {
            return true;
        }

        Optional<Cuenta> optCuenta = cuentaRepository.findById(cuentaId);
        if (optCuenta.isEmpty()) {
            return false;
        }
        Cuenta cuenta = optCuenta.get();

        String username = authentication.getName();

        // Titular directo
        if (username.equalsIgnoreCase(cuenta.getNumeroDocumento())) {
            return true;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails cud && cud.getId().equals(cuenta.getId())) {
            return true;
        }

        // Operador con permiso ADMINISTRAR_OPERADORES
        return operadorCuentaRepository.findByCuentaId(cuentaId).stream()
                .anyMatch(op -> op.getActivo() &&
                        (op.getDniOperador().equalsIgnoreCase(username) ||
                                (op.getUsuario() != null && op.getUsuario().getUsername().equalsIgnoreCase(username))) &&
                        op.getPermisos().contains(PermisoOperador.ADMINISTRAR_OPERADORES));
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
