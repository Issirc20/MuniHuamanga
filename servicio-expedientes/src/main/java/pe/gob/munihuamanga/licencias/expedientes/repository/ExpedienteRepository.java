package pe.gob.munihuamanga.licencias.expedientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad Expediente.
 */
@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, UUID> {

    Optional<Expediente> findByNumeroTramite(String numeroTramite);

    List<Expediente> findBySolicitanteId(UUID solicitanteId);

    List<Expediente> findByEstado(EstadoExpediente estado);
}
