package pe.gob.munihuamanga.licencias.expedientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.HistorialEstado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para el historial de transiciones de estado.
 */
@Repository
public interface HistorialRepository extends JpaRepository<HistorialEstado, UUID> {

    List<HistorialEstado> findByExpedienteIdOrderByFechaAsc(UUID expedienteId);

    /** Último ingreso del expediente al estado indicado. */
    Optional<HistorialEstado> findFirstByExpedienteIdAndEstadoNuevoOrderByFechaDesc(UUID expedienteId, EstadoExpediente estadoNuevo);
}
