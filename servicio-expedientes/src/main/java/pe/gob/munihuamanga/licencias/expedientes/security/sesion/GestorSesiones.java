package pe.gob.munihuamanga.licencias.expedientes.security.sesion;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.security.GeneradorTokensSeguros;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ciclo de vida de las sesiones: apertura, renovación con rotación del token y revocación.
 * La reutilización de un token de renovación ya rotado se trata como indicio de sustracción
 * y provoca la revocación de la sesión.
 */
@Service
@RequiredArgsConstructor
public class GestorSesiones {

    private final SesionUsuarioRepository repositorio;
    private final Clock reloj;

    @Value("${munihuamanga.seguridad.duracion-sesion-horas:8}")
    private long duracionSesionHoras = 8;

    /** Resultado de la apertura o renovación: la sesión y el token de renovación en claro, que se entrega una sola vez. */
    public record SesionEmitida(SesionUsuario sesion, String tokenRenovacion) {
    }

    @Transactional
    public SesionEmitida abrir(UUID usuarioId, String direccionIp, String agenteUsuario) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String token = GeneradorTokensSeguros.nuevoToken();
        SesionUsuario sesion = repositorio.save(SesionUsuario.builder()
                .id(UUID.randomUUID())
                .usuarioId(usuarioId)
                .tokenRenovacionHash(GeneradorTokensSeguros.resumen(token))
                .fechaCreacion(ahora)
                .fechaExpiracion(ahora.plus(Duration.ofHours(duracionSesionHoras)))
                .direccionIp(recortar(direccionIp, 45))
                .agenteUsuario(recortar(agenteUsuario, 255))
                .build());
        return new SesionEmitida(sesion, token);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public SesionEmitida renovar(String tokenRenovacion) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        SesionUsuario sesion = repositorio.findByTokenRenovacionHash(GeneradorTokensSeguros.resumen(tokenRenovacion))
                .orElseThrow(() -> new BadCredentialsException("La sesión no es válida."));
        if (!sesion.estaVigente(ahora)) {
            throw new BadCredentialsException("La sesión ha expirado o fue cerrada.");
        }
        String nuevo = GeneradorTokensSeguros.nuevoToken();
        sesion.setTokenRenovacionHash(GeneradorTokensSeguros.resumen(nuevo));
        sesion.setFechaUltimaRenovacion(ahora);
        repositorio.save(sesion);
        return new SesionEmitida(sesion, nuevo);
    }

    @Transactional(readOnly = true)
    public boolean estaVigente(UUID sesionId) {
        return repositorio.findById(sesionId)
                .map(sesion -> sesion.estaVigente(LocalDateTime.now(reloj)))
                .orElse(false);
    }

    @Transactional
    public void revocar(UUID sesionId, String motivo) {
        repositorio.findById(sesionId).ifPresent(sesion -> {
            sesion.setRevocada(true);
            sesion.setFechaRevocacion(LocalDateTime.now(reloj));
            sesion.setMotivoRevocacion(motivo);
            repositorio.save(sesion);
        });
    }

    @Transactional
    public void revocarPorTokenRenovacion(String tokenRenovacion, String motivo) {
        repositorio.findByTokenRenovacionHash(GeneradorTokensSeguros.resumen(tokenRenovacion))
                .ifPresent(sesion -> revocar(sesion.getId(), motivo));
    }

    @Transactional
    public int revocarTodas(UUID usuarioId, String motivo) {
        return repositorio.revocarSesionesDelUsuario(usuarioId, LocalDateTime.now(reloj), motivo);
    }

    private static String recortar(String valor, int longitud) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= longitud ? valor : valor.substring(0, longitud);
    }
}
