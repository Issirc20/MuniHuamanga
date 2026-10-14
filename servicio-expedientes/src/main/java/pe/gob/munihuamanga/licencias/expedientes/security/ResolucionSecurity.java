package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.ResolucionAdministrativaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Componente de seguridad contextual para el acceso a Resoluciones Administrativas (M09 - I08).
 */
@Slf4j
@Component("resolucionSecurity")
@RequiredArgsConstructor
public class ResolucionSecurity {

    private final ResolucionAdministrativaRepository resolucionRepository;
    private final ExpedienteSecurity expedienteSecurity;

    public boolean puedeVerResolucion(Authentication authentication, UUID resolucionId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (tieneRol(authentication, RolUsuario.ROLE_ADMIN, RolUsuario.ROLE_EVALUADOR, RolUsuario.ROLE_INSPECTOR)) {
            return true;
        }

        Optional<ResolucionAdministrativa> optRes = resolucionRepository.findById(resolucionId);
        if (optRes.isEmpty()) return false;
        ResolucionAdministrativa res = optRes.get();

        if (res.getExpediente() == null) {
            return false;
        }

        return expedienteSecurity.puedeVerExpediente(authentication, res.getExpediente().getId());
    }

    private boolean tieneRol(Authentication authentication, RolUsuario... roles) {
        for (RolUsuario rol : roles) {
            boolean match = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(rol.name()));
            if (match) return true;
        }
        return false;
    }
}
