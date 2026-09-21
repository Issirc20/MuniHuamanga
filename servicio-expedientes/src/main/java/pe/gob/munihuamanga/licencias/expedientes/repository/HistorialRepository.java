package pe.gob.munihuamanga.licencias.expedientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.model.HistorialEstado;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para el historial de transiciones de estado.
 */
@Repository
public interface HistorialRepository extends JpaRepository<HistorialEstado, UUID> {

    List<HistorialEstado> findByExpedienteIdOrderByFechaAsc(UUID expedienteId);
}
