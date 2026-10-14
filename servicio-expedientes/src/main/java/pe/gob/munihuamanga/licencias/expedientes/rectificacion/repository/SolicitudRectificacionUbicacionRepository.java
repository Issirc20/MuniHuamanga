package pe.gob.munihuamanga.licencias.expedientes.rectificacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.model.SolicitudRectificacionUbicacion;

import java.util.List;
import java.util.UUID;

@Repository
public interface SolicitudRectificacionUbicacionRepository extends JpaRepository<SolicitudRectificacionUbicacion, UUID> {

    List<SolicitudRectificacionUbicacion> findByEstablecimientoIdOrderByFechaSolicitudDesc(UUID establecimientoId);

    List<SolicitudRectificacionUbicacion> findByEstadoOrderByFechaSolicitudAsc(EstadoRectificacionUbicacion estado);
}
