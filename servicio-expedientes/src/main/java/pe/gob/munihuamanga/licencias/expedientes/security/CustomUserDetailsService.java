package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.util.UUID;

/**
 * Carga de la persona autenticada desde la base de datos.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return cargarParaSesion(username, null);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails cargarParaSesion(String username, UUID sesionId) {
        return usuarioRepository.findByUsername(username)
                .map(usuario -> new CustomUserDetails(usuario, sesionId))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales no válidas."));
    }
}
