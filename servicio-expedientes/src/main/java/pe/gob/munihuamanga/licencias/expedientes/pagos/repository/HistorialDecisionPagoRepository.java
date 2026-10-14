package pe.gob.munihuamanga.licencias.expedientes.pagos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.HistorialDecisionPago;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialDecisionPagoRepository extends JpaRepository<HistorialDecisionPago, UUID> {

    List<HistorialDecisionPago> findByPagoIdOrderByFechaDecisionDesc(UUID pagoId);
}
