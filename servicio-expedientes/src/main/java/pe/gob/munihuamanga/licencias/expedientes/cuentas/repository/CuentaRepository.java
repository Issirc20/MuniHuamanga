package pe.gob.munihuamanga.licencias.expedientes.cuentas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

    Optional<Cuenta> findByNumeroDocumento(String numeroDocumento);

    Optional<Cuenta> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    Optional<Cuenta> findByEmail(String email);

    Optional<Cuenta> findByTokenVerificacionEmail(String token);

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByEmail(String email);
}
