package pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstablecimientoRepository extends JpaRepository<Establecimiento, UUID> {

    List<Establecimiento> findByCuentaId(UUID cuentaId);

    List<Establecimiento> findByCuentaIdAndActivoTrue(UUID cuentaId);

    Optional<Establecimiento> findByCuentaIdAndCodigoEstablecimiento(UUID cuentaId, String codigoEstablecimiento);

    long countByCuentaId(UUID cuentaId);
}
