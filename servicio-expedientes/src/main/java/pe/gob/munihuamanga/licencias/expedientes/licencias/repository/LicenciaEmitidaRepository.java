package pe.gob.munihuamanga.licencias.expedientes.licencias.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.LicenciaEmitida;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LicenciaEmitidaRepository extends JpaRepository<LicenciaEmitida, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LicenciaEmitida l WHERE l.id = :id")
    Optional<LicenciaEmitida> findByIdWithLock(@Param("id") UUID id);

    Optional<LicenciaEmitida> findByExpedienteId(UUID expedienteId);

    Optional<LicenciaEmitida> findByResolucionId(UUID resolucionId);

    Optional<LicenciaEmitida> findByNumeroLicencia(String numeroLicencia);

    Optional<LicenciaEmitida> findByCodigoVerificacionQr(String codigoVerificacionQr);

    boolean existsByExpedienteId(UUID expedienteId);

    boolean existsByResolucionId(UUID resolucionId);
}
