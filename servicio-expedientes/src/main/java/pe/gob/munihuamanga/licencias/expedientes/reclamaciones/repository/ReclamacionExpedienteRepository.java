package pe.gob.munihuamanga.licencias.expedientes.reclamaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoReclamacion;
import pe.gob.munihuamanga.licencias.expedientes.reclamaciones.model.ReclamacionExpediente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReclamacionExpedienteRepository extends JpaRepository<ReclamacionExpediente, UUID> {

    List<ReclamacionExpediente> findByCuentaId(UUID cuentaId);

    List<ReclamacionExpediente> findByEstado(EstadoReclamacion estado);

    Optional<ReclamacionExpediente> findByExpedienteIdAndCuentaId(UUID expedienteId, UUID cuentaId);

    boolean existsByExpedienteIdAndEstado(UUID expedienteId, EstadoReclamacion estado);
}
