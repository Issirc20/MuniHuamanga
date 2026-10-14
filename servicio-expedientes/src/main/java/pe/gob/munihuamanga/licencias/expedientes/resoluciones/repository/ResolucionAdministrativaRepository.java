package pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResolucionAdministrativaRepository extends JpaRepository<ResolucionAdministrativa, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ResolucionAdministrativa r WHERE r.id = :id")
    Optional<ResolucionAdministrativa> findByIdWithLock(@Param("id") UUID id);

    List<ResolucionAdministrativa> findByExpedienteIdOrderByVersionDesc(UUID expedienteId);

    Optional<ResolucionAdministrativa> findByExpedienteIdAndEsVersionActualTrue(UUID expedienteId);

    Optional<ResolucionAdministrativa> findByNumeroResolucion(String numeroResolucion);

    boolean existsByExpedienteIdAndEstadoIn(UUID expedienteId, Collection<EstadoResolucion> estados);

    long countByExpedienteId(UUID expedienteId);
}
