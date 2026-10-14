package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.LicenciaEmitida;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Componente de seguridad contextual para el acceso y descarga de Licencias de Funcionamiento oficiales (M09 - I08).
 * Previene vulnerabilidades BOLA/IDOR asegurando que solo el titular, operadores autorizados o funcionarios accedan.
 */
@Slf4j
@Component("licenciaSecurity")
@RequiredArgsConstructor
public class LicenciaSecurity {

    private final LicenciaEmitidaRepository licenciaRepository;
    private final ExpedienteSecurity expedienteSecurity;

    public boolean puedeVerODescargarLicencia(Authentication authentication, UUID licenciaId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (tieneRol(authentication, RolUsuario.ROLE_ADMIN, RolUsuario.ROLE_EVALUADOR)) {
            return true;
        }

        Optional<LicenciaEmitida> optLic = licenciaRepository.findById(licenciaId);
        if (optLic.isEmpty()) return false;
        LicenciaEmitida lic = optLic.get();

        if (lic.getExpediente() == null) return false;

        return expedienteSecurity.puedeVerExpediente(authentication, lic.getExpediente().getId());
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
