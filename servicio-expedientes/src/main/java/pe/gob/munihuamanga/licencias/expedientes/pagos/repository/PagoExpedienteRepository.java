package pe.gob.munihuamanga.licencias.expedientes.pagos.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.PagoExpediente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoExpedienteRepository extends JpaRepository<PagoExpediente, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoExpediente p WHERE p.id = :id")
    Optional<PagoExpediente> findByIdWithLock(@Param("id") UUID id);

    List<PagoExpediente> findByExpedienteIdOrderByFechaRegistroDesc(UUID expedienteId);

    Optional<PagoExpediente> findByCodigoComprobante(String codigoComprobante);

    boolean existsByCodigoComprobante(String codigoComprobante);

    List<PagoExpediente> findByEstadoOrderByFechaRegistroAsc(EstadoValidacionPago estado);

    @Query("SELECT p FROM PagoExpediente p WHERE " +
           "(:estado IS NULL OR p.estado = :estado) AND " +
           "(:expedienteId IS NULL OR p.expedienteId = :expedienteId) AND " +
           "(:fechaDesde IS NULL OR p.fechaDeclarada >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR p.fechaDeclarada <= :fechaHasta) " +
           "ORDER BY p.fechaRegistro DESC")
    List<PagoExpediente> buscarConFiltros(
            @Param("estado") EstadoValidacionPago estado,
            @Param("expedienteId") UUID expedienteId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta
    );
}
