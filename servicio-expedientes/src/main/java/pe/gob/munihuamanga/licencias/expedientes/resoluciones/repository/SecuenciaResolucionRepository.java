package pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.SecuenciaResolucion;

import java.util.Optional;

@Repository
public interface SecuenciaResolucionRepository extends JpaRepository<SecuenciaResolucion, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaResolucion s WHERE s.anio = :anio")
    Optional<SecuenciaResolucion> findByIdWithLock(@Param("anio") Integer anio);
}
