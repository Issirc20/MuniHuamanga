package pe.gob.munihuamanga.licencias.expedientes.documentos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentoVersionadoRepository extends JpaRepository<DocumentoVersionado, UUID> {

    List<DocumentoVersionado> findByExpedienteIdAndEsVersionActualTrue(UUID expedienteId);

    List<DocumentoVersionado> findByExpedienteIdOrderByTipoDocumentoAscVersionDesc(UUID expedienteId);

    List<DocumentoVersionado> findByExpedienteIdAndTipoDocumentoOrderByVersionDesc(UUID expedienteId, TipoDocumentoAdjunto tipoDocumento);

    Optional<DocumentoVersionado> findFirstByExpedienteIdAndTipoDocumentoOrderByVersionDesc(UUID expedienteId, TipoDocumentoAdjunto tipoDocumento);

    List<DocumentoVersionado> findByBorradorIdAndEsVersionActualTrue(UUID borradorId);

    List<DocumentoVersionado> findByBorradorIdAndTipoDocumentoOrderByVersionDesc(UUID borradorId, TipoDocumentoAdjunto tipoDocumento);

    Optional<DocumentoVersionado> findFirstByBorradorIdAndTipoDocumentoOrderByVersionDesc(UUID borradorId, TipoDocumentoAdjunto tipoDocumento);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE DocumentoVersionado d SET d.esVersionActual = false WHERE d.expedienteId = :expedienteId AND d.tipoDocumento = :tipoDocumento")
    void desmarcarVersionesActualesExpediente(UUID expedienteId, TipoDocumentoAdjunto tipoDocumento);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE DocumentoVersionado d SET d.esVersionActual = false WHERE d.borradorId = :borradorId AND d.tipoDocumento = :tipoDocumento")
    void desmarcarVersionesActualesBorrador(UUID borradorId, TipoDocumentoAdjunto tipoDocumento);
}
