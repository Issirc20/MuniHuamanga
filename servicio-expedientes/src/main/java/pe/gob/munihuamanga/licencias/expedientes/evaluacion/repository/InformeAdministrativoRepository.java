package pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InformeAdministrativoRepository extends JpaRepository<InformeAdministrativo, UUID> {

    Optional<InformeAdministrativo> findByExpedienteIdAndEsVersionActualTrue(UUID expedienteId);

    List<InformeAdministrativo> findByExpedienteIdOrderByVersionDesc(UUID expedienteId);

    Optional<InformeAdministrativo> findByNumeroInformeAndVersion(String numeroInforme, Integer version);

    List<InformeAdministrativo> findByEstado(EstadoInforme estado);

    @Query("SELECT COUNT(i) FROM InformeAdministrativo i WHERE i.numeroInforme LIKE CONCAT(:prefix, '%')")
    long countByNumeroInformePrefix(@Param("prefix") String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InformeAdministrativo i WHERE i.id = :id")
    Optional<InformeAdministrativo> findByIdWithLock(@Param("id") UUID id);
}
