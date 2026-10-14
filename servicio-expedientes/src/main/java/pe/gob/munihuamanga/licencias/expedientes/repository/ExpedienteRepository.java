package pe.gob.munihuamanga.licencias.expedientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad Expediente.
 */
@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, UUID> {

    Optional<Expediente> findByNumeroTramite(String numeroTramite);

    List<Expediente> findBySolicitanteId(UUID solicitanteId);

    List<Expediente> findByEstado(EstadoExpediente estado);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Expediente e WHERE e.id = :id")
    Optional<Expediente> findByIdWithLock(@org.springframework.data.repository.query.Param("id") UUID id);

    org.springframework.data.domain.Page<Expediente> findByEstadoIn(java.util.Collection<EstadoExpediente> estados, org.springframework.data.domain.Pageable pageable);

    Optional<Expediente> findByLicenciaQrCode(String licenciaQrCode);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Expediente e WHERE " +
           "(:estado IS NULL OR e.estado = :estado) AND " +
           "(:nivelRiesgo IS NULL OR e.nivelRiesgo = :nivelRiesgo) AND " +
           "(:busqueda IS NULL OR LOWER(e.numeroTramite) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(e.nombreTitular) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(e.documentoIdentidad) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR (e.razonSocial IS NOT NULL AND LOWER(e.razonSocial) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "OR (e.nombreComercial IS NOT NULL AND LOWER(e.nombreComercial) LIKE LOWER(CONCAT('%', :busqueda, '%'))))")
    org.springframework.data.domain.Page<Expediente> buscarConFiltros(
            @org.springframework.data.repository.query.Param("estado") EstadoExpediente estado,
            @org.springframework.data.repository.query.Param("nivelRiesgo") pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo nivelRiesgo,
            @org.springframework.data.repository.query.Param("busqueda") String busqueda,
            org.springframework.data.domain.Pageable pageable);
}
