package pe.gob.munihuamanga.licencias.expedientes.zonificacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.model.EvaluacionZonificacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluacionZonificacionRepository extends JpaRepository<EvaluacionZonificacion, UUID> {

    List<EvaluacionZonificacion> findByExpedienteIdOrderByFechaRegistroDesc(UUID expedienteId);

    Optional<EvaluacionZonificacion> findFirstByExpedienteIdOrderByFechaRegistroDesc(UUID expedienteId);

    List<EvaluacionZonificacion> findByResultadoOrderByFechaRegistroAsc(ResultadoZonificacion resultado);
}
