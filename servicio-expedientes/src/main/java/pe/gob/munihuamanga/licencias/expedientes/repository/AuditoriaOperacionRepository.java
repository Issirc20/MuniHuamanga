package pe.gob.munihuamanga.licencias.expedientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.model.AuditoriaOperacion;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la auditoría transversal unificada de operaciones (M12).
 */
@Repository
public interface AuditoriaOperacionRepository extends JpaRepository<AuditoriaOperacion, UUID> {

    List<AuditoriaOperacion> findByModuloOrderByFechaHoraDesc(String modulo);

    List<AuditoriaOperacion> findByEntidadIdOrderByFechaHoraDesc(UUID entidadId);

    List<AuditoriaOperacion> findByUsuarioOrderByFechaHoraDesc(String usuario);

    List<AuditoriaOperacion> findByModuloAndEntidadIdOrderByFechaHoraDesc(String modulo, UUID entidadId);
}
