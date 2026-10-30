package pe.gob.munihuamanga.licencias.expedientes.security.sesion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, UUID> {

    Optional<SesionUsuario> findByTokenRenovacionHash(String tokenRenovacionHash);

    @Modifying
    @Query("UPDATE SesionUsuario s SET s.revocada = true, s.fechaRevocacion = :momento, s.motivoRevocacion = :motivo "
            + "WHERE s.usuarioId = :usuarioId AND s.revocada = false")
    int revocarSesionesDelUsuario(@Param("usuarioId") UUID usuarioId,
                                  @Param("momento") LocalDateTime momento,
                                  @Param("motivo") String motivo);
}
