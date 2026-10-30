package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación transitoria del puerto: el sistema aún no registra recursos administrativos.
 * Debe sustituirse por la implementación del módulo de reclamaciones cuando este los gestione.
 */
@Component
public class SinRegistroDeRecursosAdministrativos implements ConsultaRecursosAdministrativos {

    @Override
    public boolean existenRecursosPendientes(UUID expedienteId) {
        return false;
    }
}
