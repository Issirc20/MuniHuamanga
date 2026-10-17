package pe.gob.munihuamanga.licencias.expedientes.plantillas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.model.PlantillaDocumentalVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantillaDocumentalVersionRepository extends JpaRepository<PlantillaDocumentalVersion, UUID> {
    List<PlantillaDocumentalVersion> findByPlantillaIdOrderByVersionDesc(UUID plantillaId);
    List<PlantillaDocumentalVersion> findByCodigoPlantillaOrderByVersionDesc(String codigoPlantilla);
    Optional<PlantillaDocumentalVersion> findByPlantillaIdAndVersion(UUID plantillaId, Integer version);
}
