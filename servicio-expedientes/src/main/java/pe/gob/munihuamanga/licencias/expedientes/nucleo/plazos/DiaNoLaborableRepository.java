package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiaNoLaborableRepository extends JpaRepository<DiaNoLaborable, LocalDate> {

    List<DiaNoLaborable> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}
