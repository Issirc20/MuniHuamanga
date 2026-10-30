package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository;

import java.util.UUID;

/**
 * Política de acceso a las licencias emitidas: se aplica la misma regla que al expediente de origen.
 */
@Component("licenciaSecurity")
@RequiredArgsConstructor
public class LicenciaSecurity {

    private final LicenciaEmitidaRepository licenciaRepository;
    private final ExpedienteSecurity expedienteSecurity;

    public boolean puedeVerODescargarLicencia(Authentication authentication, UUID licenciaId) {
        if (licenciaId == null) {
            return false;
        }
        return licenciaRepository.findById(licenciaId)
                .filter(licencia -> licencia.getExpediente() != null)
                .map(licencia -> expedienteSecurity.puedeVerExpediente(authentication, licencia.getExpediente().getId()))
                .orElse(false);
    }
}
