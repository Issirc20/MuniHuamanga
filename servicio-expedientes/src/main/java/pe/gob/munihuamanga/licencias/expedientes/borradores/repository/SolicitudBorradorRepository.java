package pe.gob.munihuamanga.licencias.expedientes.borradores.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolicitudBorradorRepository extends JpaRepository<SolicitudBorrador, UUID> {

    List<SolicitudBorrador> findByCuentaId(UUID cuentaId);

    List<SolicitudBorrador> findByCuentaIdAndEstadoNot(UUID cuentaId, EstadoBorrador estado);

    @Query("SELECT b FROM SolicitudBorrador b LEFT JOIN FETCH b.documentos WHERE b.id = :id")
    Optional<SolicitudBorrador> findByIdWithDocumentos(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM SolicitudBorrador b WHERE b.id = :id")
    Optional<SolicitudBorrador> findByIdWithLock(@Param("id") UUID id);

    boolean existsByEstablecimientoIdAndEstadoIn(UUID establecimientoId, List<EstadoBorrador> estados);
}
