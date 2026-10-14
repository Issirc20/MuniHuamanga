package pe.gob.munihuamanga.licencias.expedientes.licencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.AuditoriaConsultaInspeccion;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaConsultaInspeccionRepository extends JpaRepository<AuditoriaConsultaInspeccion, UUID> {

    List<AuditoriaConsultaInspeccion> findByLicenciaIdOrderByFechaConsultaDesc(UUID licenciaId);

    List<AuditoriaConsultaInspeccion> findByInspectorUsernameOrderByFechaConsultaDesc(String inspectorUsername);
}
