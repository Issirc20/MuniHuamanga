package pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.CatalogoCiiu;

import java.util.List;

@Repository
public interface CatalogoCiiuRepository extends JpaRepository<CatalogoCiiu, String> {

    List<CatalogoCiiu> findByActivoTrueOrderByCodigoAsc();
}
