package pe.gob.munihuamanga.licencias.expedientes.itse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoItse;
import pe.gob.munihuamanga.licencias.expedientes.itse.model.ExpedienteItse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpedienteItseRepository extends JpaRepository<ExpedienteItse, UUID> {

    Optional<ExpedienteItse> findByExpedienteId(UUID expedienteId);

    boolean existsByExpedienteId(UUID expedienteId);

    List<ExpedienteItse> findByEstadoOrderByFechaCargaAsc(EstadoItse estado);
}
