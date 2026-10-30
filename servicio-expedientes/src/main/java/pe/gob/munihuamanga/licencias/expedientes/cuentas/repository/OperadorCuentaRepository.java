package pe.gob.munihuamanga.licencias.expedientes.cuentas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OperadorCuentaRepository extends JpaRepository<OperadorCuenta, UUID> {

    List<OperadorCuenta> findByCuentaId(UUID cuentaId);

    Optional<OperadorCuenta> findByCuentaIdAndDniOperador(UUID cuentaId, String dniOperador);

    List<OperadorCuenta> findByDniOperador(String dniOperador);

    List<OperadorCuenta> findByUsuarioId(UUID usuarioId);

    List<OperadorCuenta> findByUsuarioIdAndActivoTrue(UUID usuarioId);

    Optional<OperadorCuenta> findByCuentaIdAndUsuarioId(UUID cuentaId, UUID usuarioId);

    Optional<OperadorCuenta> findByCuentaIdAndUsuarioIdAndActivoTrue(UUID cuentaId, UUID usuarioId);

    boolean existsByCuentaIdAndDniOperador(UUID cuentaId, String dniOperador);
}
