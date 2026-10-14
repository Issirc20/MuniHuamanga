package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.borradores.security.DocumentoSecurityValidator;
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
    private final pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository operadorCuentaRepository;
    private final pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository cuentaRepository;

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

    /**
     * Verifica la vinculación de titularidad legítima entre el usuario autenticado y el expediente.
     * En MuniHuamanga V2 (Auditoría I05.1):
     * - Exige estrictamente la vinculación de identidad persistente Cuenta <-> Expediente (solicitanteId == usuarioId o cuentaId).
     * - Si usuarioId no está en el token (p. ej. en pruebas), resuelve la Cuenta persistente por authUsername y valida el UUID.
     * - Admite operadores nominativos activos con permiso CONSULTAR_EXPEDIENTE.
     * - REGLA DE SEGURIDAD OBLIGATORIA I05.1: Se elimina tajantemente la autorización por coincidencia simple entre
     *   username y documentoIdentidad. Los expedientes V1 sin titularidad verificada (solicitanteId y cuentaId nulos)
     *   permanecen bajo acceso municipal restringido hasta su reclamación y vinculación formal (I04).
     */
    private boolean esPropietarioDeExpediente(Authentication authentication, Expediente exp) {
        if (!estaAutenticado(authentication) || exp == null) {
            return false;
        }

        final String authUsername = (authentication.getPrincipal() instanceof CustomUserDetails cud)
                ? cud.getUsername()
                : authentication.getName();
        UUID usuarioId = (authentication.getPrincipal() instanceof CustomUserDetails cud)
                ? cud.getId()
                : null;

        // Si usuarioId no está en el principal (p. ej. autenticación mock), resolver por cuenta persistente
        if (usuarioId == null && authUsername != null) {
            usuarioId = cuentaRepository.findByNumeroDocumento(authUsername)
                    .map(pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta::getId)
                    .orElse(null);
        }

        // 1. Vinculación estricta y persistente de titularidad (Cuenta.id == Expediente.solicitanteId o cuentaId)
        if (usuarioId != null) {
            if (exp.getSolicitanteId() != null && usuarioId.equals(exp.getSolicitanteId())) {
                return true;
            }
            if (exp.getCuentaId() != null && usuarioId.equals(exp.getCuentaId())) {
                return true;
            }
        }

        // 2. Operador nominativo de la cuenta asociada al expediente (I03)
        if (exp.getCuentaId() != null) {
            final UUID finalUsuarioId = usuarioId;
            boolean esOperadorAutorizado = operadorCuentaRepository.findByCuentaId(exp.getCuentaId()).stream()
                    .anyMatch(op -> Boolean.TRUE.equals(op.getActivo()) &&
                            (op.getDniOperador().equalsIgnoreCase(authUsername) ||
                                    (op.getUsuario() != null && op.getUsuario().getUsername().equalsIgnoreCase(authUsername)) ||
                                    (finalUsuarioId != null && op.getUsuario() != null && finalUsuarioId.equals(op.getUsuario().getId()))) &&
                            op.getPermisos().contains(pe.gob.munihuamanga.licencias.common.enums.PermisoOperador.CONSULTAR_EXPEDIENTE));
            if (esOperadorAutorizado) {
                return true;
            }
        }

        // Si el expediente no tiene solicitanteId ni cuentaId (expediente histórico V1 sin reclamar),
        // ningún ciudadano tiene acceso directo; requiere resolución formal de reclamación de titularidad.
        log.warn("[SEGURIDAD - TITULARIDAD] Acceso denegado: No existe vinculación de titularidad persistente verificada para usuario {} en expediente {}",
                DocumentoSecurityValidator.ofuscarDocumento(authUsername), exp.getId());
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
