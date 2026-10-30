package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Bloqueo temporal persistido en la cuenta de usuario, válido aun con varias instancias del sistema.
 * Tras el número máximo de intentos fallidos consecutivos, el acceso se bloquea durante el tiempo configurado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProteccionIntentosAccesoPersistente implements ProteccionIntentosAcceso {

    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;

    @Value("${munihuamanga.seguridad.intentos-maximos:5}")
    private int intentosMaximos = 5;

    @Value("${munihuamanga.seguridad.minutos-bloqueo:15}")
    private int minutosBloqueo = 15;

    @Override
    @Transactional(readOnly = true)
    public boolean estaBloqueado(String username) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return usuarioRepository.findByUsername(username)
                .map(usuario -> usuario.estaBloqueado(ahora))
                .orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(String username) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= intentosMaximos) {
                usuario.setBloqueadoHasta(LocalDateTime.now(reloj).plusMinutes(minutosBloqueo));
                usuario.setIntentosFallidos(0);
                log.warn("[SEGURIDAD] Acceso bloqueado temporalmente por {} minutos tras intentos fallidos reiterados.", minutosBloqueo);
            }
            usuarioRepository.save(usuario);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(String username) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuario.setFechaUltimoAcceso(LocalDateTime.now(reloj));
            usuarioRepository.save(usuario);
        });
    }
}
