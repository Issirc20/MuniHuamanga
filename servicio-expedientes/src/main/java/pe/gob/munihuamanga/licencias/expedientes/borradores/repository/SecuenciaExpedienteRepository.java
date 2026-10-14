package pe.gob.munihuamanga.licencias.expedientes.borradores.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SecuenciaExpediente;

import java.util.Optional;

@Repository
public interface SecuenciaExpedienteRepository extends JpaRepository<SecuenciaExpediente, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaExpediente s WHERE s.anio = :anio")
    Optional<SecuenciaExpediente> findByAnioWithLock(@Param("anio") Integer anio);
}
