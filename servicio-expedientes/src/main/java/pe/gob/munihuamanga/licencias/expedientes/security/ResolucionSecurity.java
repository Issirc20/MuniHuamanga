package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.ResolucionAdministrativaRepository;

import java.util.UUID;

/**
 * Política de acceso a las resoluciones: se aplica la misma regla que al expediente de origen.
 */
@Component("resolucionSecurity")
@RequiredArgsConstructor
public class ResolucionSecurity {

    private final ResolucionAdministrativaRepository resolucionRepository;
    private final ExpedienteSecurity expedienteSecurity;

    public boolean puedeVerResolucion(Authentication authentication, UUID resolucionId) {
        if (resolucionId == null) {
            return false;
        }
        return resolucionRepository.findById(resolucionId)
                .filter(resolucion -> resolucion.getExpediente() != null)
                .map(resolucion -> expedienteSecurity.puedeVerExpediente(authentication, resolucion.getExpediente().getId()))
                .orElse(false);
    }
}
