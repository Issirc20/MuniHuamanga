package pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.HistorialInformeAdministrativo;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialInformeAdministrativoRepository extends JpaRepository<HistorialInformeAdministrativo, UUID> {

    List<HistorialInformeAdministrativo> findByInformeIdOrderByFechaAsc(UUID informeId);
}
