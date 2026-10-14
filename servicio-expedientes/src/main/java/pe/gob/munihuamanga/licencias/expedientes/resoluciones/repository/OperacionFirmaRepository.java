package pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.OperacionFirma;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OperacionFirmaRepository extends JpaRepository<OperacionFirma, UUID> {

    List<OperacionFirma> findByResolucionIdOrderByFechaInicioDesc(UUID resolucionId);

    Optional<OperacionFirma> findByIdTransaccionRemota(String idTransaccionRemota);
}
