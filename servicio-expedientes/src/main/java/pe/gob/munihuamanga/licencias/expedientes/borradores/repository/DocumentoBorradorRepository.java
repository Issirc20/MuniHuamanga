package pe.gob.munihuamanga.licencias.expedientes.borradores.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.DocumentoBorrador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentoBorradorRepository extends JpaRepository<DocumentoBorrador, UUID> {

    List<DocumentoBorrador> findByBorradorId(UUID borradorId);

    Optional<DocumentoBorrador> findByBorradorIdAndTipoDocumento(UUID borradorId, TipoDocumentoAdjunto tipoDocumento);

    void deleteByBorradorId(UUID borradorId);
}
