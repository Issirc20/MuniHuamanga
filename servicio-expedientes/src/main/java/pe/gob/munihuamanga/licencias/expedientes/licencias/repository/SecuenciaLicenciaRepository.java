package pe.gob.munihuamanga.licencias.expedientes.licencias.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.SecuenciaLicencia;

import java.util.Optional;

@Repository
public interface SecuenciaLicenciaRepository extends JpaRepository<SecuenciaLicencia, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaLicencia s WHERE s.anio = :anio")
    Optional<SecuenciaLicencia> findByIdWithLock(@Param("anio") Integer anio);
}
