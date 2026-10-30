package pe.gob.munihuamanga.licencias.expedientes.itse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.itse.model.ObservacionItse;

import java.util.List;
import java.util.UUID;

@Repository
public interface ObservacionItseRepository extends JpaRepository<ObservacionItse, UUID> {

    List<ObservacionItse> findByExpedienteItseIdOrderByFechaObservacionDesc(UUID expedienteItseId);

    List<ObservacionItse> findByExpedienteItseIdAndAtendidaFalse(UUID expedienteItseId);
}
