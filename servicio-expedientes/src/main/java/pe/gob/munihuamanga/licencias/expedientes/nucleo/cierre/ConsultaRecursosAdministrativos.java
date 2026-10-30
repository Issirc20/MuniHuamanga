package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

import java.util.UUID;

/**
 * Puerto de consulta de recursos administrativos (reconsideración, apelación) en trámite.
 * La gestión de recursos se incorpora con el módulo de reclamaciones; mientras tanto,
 * la implementación vigente informa que no existen recursos registrados.
 */
public interface ConsultaRecursosAdministrativos {

    boolean existenRecursosPendientes(UUID expedienteId);
}
