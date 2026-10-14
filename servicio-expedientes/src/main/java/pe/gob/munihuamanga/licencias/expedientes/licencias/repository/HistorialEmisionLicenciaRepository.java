package pe.gob.munihuamanga.licencias.expedientes.licencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.HistorialEmisionLicencia;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialEmisionLicenciaRepository extends JpaRepository<HistorialEmisionLicencia, UUID> {

    List<HistorialEmisionLicencia> findByLicenciaIdOrderByFechaRegistroDesc(UUID licenciaId);
}
