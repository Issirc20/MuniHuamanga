package pe.gob.munihuamanga.licencias.expedientes.plantillas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.model.PlantillaDocumental;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantillaDocumentalRepository extends JpaRepository<PlantillaDocumental, UUID> {
    Optional<PlantillaDocumental> findByCodigo(String codigo);
    List<PlantillaDocumental> findByActivoTrueOrderByCodigoAsc();
    List<PlantillaDocumental> findByGrupoAndActivoTrueOrderByCodigoAsc(String grupo);
}
