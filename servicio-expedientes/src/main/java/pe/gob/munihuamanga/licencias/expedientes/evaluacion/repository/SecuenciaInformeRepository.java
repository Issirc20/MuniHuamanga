package pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.SecuenciaInforme;

import java.util.Optional;

@Repository
public interface SecuenciaInformeRepository extends JpaRepository<SecuenciaInforme, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaInforme s WHERE s.anio = :anio")
    Optional<SecuenciaInforme> findByAnioWithLock(@Param("anio") Integer anio);
}
