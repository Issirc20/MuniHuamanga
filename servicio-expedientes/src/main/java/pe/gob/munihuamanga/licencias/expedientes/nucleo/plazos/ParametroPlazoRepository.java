package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametroPlazoRepository extends JpaRepository<ParametroPlazo, CodigoPlazo> {
}
