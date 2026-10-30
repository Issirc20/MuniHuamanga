package pe.gob.munihuamanga.licencias.expedientes.cuentas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.EstablecerContrasenaDto;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.GeneradorTokensSeguros;
import pe.gob.munihuamanga.licencias.expedientes.security.PoliticaContrasenas;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Activación de accesos mediante enlace de un solo uso: la persona define su propia contraseña.
 * Se aplica a los operadores de cuentas y a los funcionarios dados de alta por la administración.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivacionAccesoService {

    private static final long HORAS_VIGENCIA = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasenas politicaContrasenas;
    private final PuertoNotificacionCuenta notificacionCuenta;
    private final Clock reloj;

    @Value("${notificacion.email.portal-ciudadano:http://localhost:8081}")
    private String urlPortal;

    /**
     * Deja el acceso pendiente de activación y envía el enlace a la persona.
     * La contraseña previa queda inutilizable hasta la activación.
     */
    @Transactional
    public void solicitarActivacion(Usuario usuario) {
        String token = GeneradorTokensSeguros.nuevoToken();
        usuario.setPassword(passwordEncoder.encode(GeneradorTokensSeguros.nuevoToken()));
        usuario.setPendienteActivacion(true);
        usuario.setTokenActivacionHash(GeneradorTokensSeguros.resumen(token));
        usuario.setTokenActivacionExpira(LocalDateTime.now(reloj).plusHours(HORAS_VIGENCIA));
        usuarioRepository.save(usuario);
        notificacionCuenta.enviarActivacionAcceso(usuario.getEmail(), usuario.getNombreCompleto(),
                urlPortal + "/activar-acceso.html?token=" + token);
    }

    @Transactional
    public void activar(EstablecerContrasenaDto dto) {
        Usuario usuario = usuarioRepository.findByTokenActivacionHash(GeneradorTokensSeguros.resumen(dto.getToken()))
                .filter(candidato -> candidato.getTokenActivacionExpira() != null
                        && candidato.getTokenActivacionExpira().isAfter(LocalDateTime.now(reloj)))
                .orElseThrow(() -> new IllegalArgumentException("El enlace de activación no es válido o ha vencido."));

        politicaContrasenas.exigir(dto.getPassword(), usuario.getUsername());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setPendienteActivacion(false);
        usuario.setTokenActivacionHash(null);
        usuario.setTokenActivacionExpira(null);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        log.info("[CUENTAS] Acceso activado para el usuario {}.", usuario.getId());
    }
}
