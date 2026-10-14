package pe.gob.munihuamanga.licencias.expedientes.cuentas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.AuditoriaCuentaPermiso;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaCuentaPermisoRepository extends JpaRepository<AuditoriaCuentaPermiso, UUID> {

    List<AuditoriaCuentaPermiso> findByCuentaIdOrderByFechaDesc(UUID cuentaId);

    List<AuditoriaCuentaPermiso> findByOperadorIdOrderByFechaDesc(UUID operadorId);
}
