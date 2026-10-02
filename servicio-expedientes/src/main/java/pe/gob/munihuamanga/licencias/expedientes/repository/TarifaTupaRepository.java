package pe.gob.munihuamanga.licencias.expedientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.model.TarifaTupa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para tarifas TUPA.
 */
@Repository
public interface TarifaTupaRepository extends JpaRepository<TarifaTupa, UUID> {

    Optional<TarifaTupa> findByNivelRiesgo(NivelRiesgo nivelRiesgo);

    Optional<TarifaTupa> findByCodigoTupa(String codigoTupa);

    List<TarifaTupa> findByActivoTrueOrderByMontoTotalAsc();
}
