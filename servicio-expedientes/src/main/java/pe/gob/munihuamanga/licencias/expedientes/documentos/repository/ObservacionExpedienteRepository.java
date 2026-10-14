package pe.gob.munihuamanga.licencias.expedientes.documentos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.ObservacionExpediente;

import java.util.List;
import java.util.UUID;

@Repository
public interface ObservacionExpedienteRepository extends JpaRepository<ObservacionExpediente, UUID> {

    List<ObservacionExpediente> findByExpedienteIdOrderByFechaObservacionDesc(UUID expedienteId);

    List<ObservacionExpediente> findByExpedienteIdAndSubsanadaFalse(UUID expedienteId);

    long countByExpedienteIdAndSubsanadaFalse(UUID expedienteId);

    boolean existsByExpedienteIdAndSubsanadaFalse(UUID expedienteId);
}
