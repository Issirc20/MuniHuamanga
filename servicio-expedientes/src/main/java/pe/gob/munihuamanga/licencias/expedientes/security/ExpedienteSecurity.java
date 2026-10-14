package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Componente de autorización a nivel de objeto para expedientes (BOLA / IDOR).
 * Valida la identidad del actor, propiedad del expediente, rol municipal y estado del trámite
 * según las directrices de seguridad de MuniHuamanga V2 (DAS DA-02 / ERS RF-USR-006 / CA-01).
 */
@Slf4j
@Component("expedienteSecurity")
@RequiredArgsConstructor
public class ExpedienteSecurity {

    private final ExpedienteRepository expedienteRepository;

    /**
     * Comprueba si el usuario autenticado tiene autorización para consultar el expediente completo.
     */
    public boolean puedeVerExpediente(Authentication authentication, UUID expedienteId) {
        if (!estaAutenticado(authentication) || expedienteId == null) {
            return false;
        }

        if (esFuncionarioAutorizado(authentication)) {
            return true;
        }

        // Si es inspector, no tiene acceso directo a expedientes internos (solo QR ampliado)
        if (tieneRol(authentication, RolUsuario.ROLE_INSPECTOR)) {
            log.warn("[SEGURIDAD - IDOR] Inspector {} intentó acceder a expediente interno {}", authentication.getName(), expedienteId);
            return false;
        }

        // Si es ciudadano, solo puede ver su propio expediente
        if (tieneRol(authentication, RolUsuario.ROLE_CIUDADANO)) {
            return esPropietario(authentication, expedienteId);
        }

        return false;
    }

    /**
     * Comprueba si el usuario autenticado tiene autorización para consultar el expediente por número de trámite.
     */
    public boolean puedeVerExpedientePorTramite(Authentication authentication, String numeroTramite) {
        if (!estaAutenticado(authentication) || numeroTramite == null || numeroTramite.isBlank()) {
            return false;
        }

        if (esFuncionarioAutorizado(authentication)) {
            return true;
        }

        if (tieneRol(authentication, RolUsuario.ROLE_INSPECTOR)) {
            log.warn("[SEGURIDAD - IDOR] Inspector {} intentó acceder a detalle de trámite {}", authentication.getName(), numeroTramite);
            return false;
        }

        if (tieneRol(authentication, RolUsuario.ROLE_CIUDADANO)) {
            Optional<Expediente> opt = expedienteRepository.findByNumeroTramite(numeroTramite.trim());
            return opt.map(exp -> esPropietarioDeExpediente(authentication, exp)).orElse(false);
        }

        return false;
    }

    /**
     * Comprueba si el usuario autenticado puede descargar documentos oficiales asociados al expediente.
     */
    public boolean puedeDescargarDocumento(Authentication authentication, UUID expedienteId) {
        if (!estaAutenticado(authentication) || expedienteId == null) {
            return false;
        }

        if (esFuncionarioAutorizado(authentication)) {
            return true;
        }

        if (tieneRol(authentication, RolUsuario.ROLE_INSPECTOR)) {
            log.warn("[SEGURIDAD - IDOR] Inspector {} intentó descargar documentos del expediente {}", authentication.getName(), expedienteId);
            return false;
        }

        if (tieneRol(authentication, RolUsuario.ROLE_CIUDADANO)) {
            return esPropietario(authentication, expedienteId);
        }

        return false;
    }

    /**
     * Comprueba si el usuario autenticado puede presentar subsanaciones sobre el expediente.
     * Exige:
     * 1. Ser el titular legítimo del trámite (o funcionario evaluador/admin facultado).
     * 2. Que el expediente se encuentre efectivamente en estado OBSERVADO (Ley N° 27444 LPAG).
     */
    public boolean puedeSubsanar(Authentication authentication, UUID expedienteId) {
        if (!estaAutenticado(authentication) || expedienteId == null) {
            return false;
        }

        Optional<Expediente> opt = expedienteRepository.findById(expedienteId);
        if (opt.isEmpty()) {
            return false;
        }

        Expediente expediente = opt.get();

        // Regla de estado: Solo se subsana si está en estado OBSERVADO
        if (expediente.getEstado() != EstadoExpediente.OBSERVADO) {
            log.warn("[SEGURIDAD] Intento de subsanar expediente {} en estado no OBSERVADO ({})", expedienteId, expediente.getEstado());
            return false;
        }

        // Funcionarios evaluadores o administradores pueden registrar subsanación por mesa de partes
        if (tieneRol(authentication, RolUsuario.ROLE_ADMIN) || tieneRol(authentication, RolUsuario.ROLE_EVALUADOR)) {
            return true;
        }

        // Ciudadano: Debe ser el propietario estricto del expediente
        if (tieneRol(authentication, RolUsuario.ROLE_CIUDADANO)) {
            boolean propietario = esPropietarioDeExpediente(authentication, expediente);
            if (!propietario) {
                log.warn("[SEGURIDAD - IDOR] Ciudadano {} intentó subsanar expediente ajeno {}", authentication.getName(), expedienteId);
            }
            return propietario;
        }

        return false;
    }

    /**
     * Comprueba si el usuario autenticado es funcionario autorizado (ADMIN, EVALUADOR o CAJERO).
     */
    public boolean esFuncionarioAutorizado(Authentication authentication) {
        return tieneRol(authentication, RolUsuario.ROLE_ADMIN)
                || tieneRol(authentication, RolUsuario.ROLE_EVALUADOR)
                || tieneRol(authentication, RolUsuario.ROLE_CAJERO);
    }

    /**
     * Comprueba si el usuario autenticado es evaluador o administrador (capacidad de dictamen).
     */
    public boolean esEvaluadorOAdmin(Authentication authentication) {
        return tieneRol(authentication, RolUsuario.ROLE_ADMIN)
                || tieneRol(authentication, RolUsuario.ROLE_EVALUADOR);
    }

    /**
     * Comprueba si el usuario autenticado es cajero o administrador (capacidad de cobro y registro de pagos).
     */
    public boolean esCajeroOAdmin(Authentication authentication) {
        return tieneRol(authentication, RolUsuario.ROLE_ADMIN)
                || tieneRol(authentication, RolUsuario.ROLE_CAJERO);
    }

    /**
     * Verifica si el usuario autenticado es el propietario legítimo del expediente.
     */
    public boolean esPropietario(Authentication authentication, UUID expedienteId) {
        if (expedienteId == null) return false;
        return expedienteRepository.findById(expedienteId)
                .map(exp -> esPropietarioDeExpediente(authentication, exp))
                .orElse(false);
    }

    private boolean esPropietarioDeExpediente(Authentication authentication, Expediente exp) {
        if (!estaAutenticado(authentication) || exp == null) {
            return false;
        }

        String username = authentication.getName();
        UUID usuarioId = null;

        if (authentication.getPrincipal() instanceof CustomUserDetails cud) {
            usuarioId = cud.getId();
            username = cud.getUsername();
        }

        // Comprobación por UUID de solicitante
        if (usuarioId != null && exp.getSolicitanteId() != null && usuarioId.equals(exp.getSolicitanteId())) {
            return true;
        }

        // Comprobación por documento de identidad (DNI o RUC con el que inició sesión)
        if (username != null && exp.getDocumentoIdentidad() != null
                && username.trim().equalsIgnoreCase(exp.getDocumentoIdentidad().trim())) {
            return true;
        }

        return false;
    }

    private boolean estaAutenticado(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean tieneRol(Authentication authentication, RolUsuario rol) {
        if (!estaAutenticado(authentication) || rol == null) {
            return false;
        }
        String nombreRol = rol.name();
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            if (nombreRol.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
