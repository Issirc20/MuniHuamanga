package pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.HistorialResolucion;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialResolucionRepository extends JpaRepository<HistorialResolucion, UUID> {

    List<HistorialResolucion> findByResolucionIdOrderByFechaRegistroDesc(UUID resolucionId);
}
