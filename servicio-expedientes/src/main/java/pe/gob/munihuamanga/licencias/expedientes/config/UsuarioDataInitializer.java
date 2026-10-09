package pe.gob.munihuamanga.licencias.expedientes.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inicializador de usuarios semilla para desarrollo y pruebas del sistema municipal.
 * Excluido del entorno de producción ('prod') por razones de seguridad (H02).
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class UsuarioDataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste(
                "admin",
                "admin123",
                "Lic. Fernando Quispe - Administrador del Sistema",
                "admin.licencias@munihuamanga.gob.pe",
                RolUsuario.ROLE_ADMIN
        );

        crearUsuarioSiNoExiste(
                "evaluador",
                "eval123",
                "Ing. Carlos Mendoza - Subgerencia de Comercio y Licencias",
                "carlos.mendoza@munihuamanga.gob.pe",
                RolUsuario.ROLE_EVALUADOR
        );

        crearUsuarioSiNoExiste(
                "cajero",
                "caja123",
                "Rosa Flores - Ventanilla de Recaudación SAT Huamanga",
                "rosa.flores@sat-huamanga.gob.pe",
                RolUsuario.ROLE_CAJERO
        );
    }

    private void crearUsuarioSiNoExiste(
            String username,
            String rawPassword,
            String nombreCompleto,
            String email,
            RolUsuario rol
    ) {
        if (!usuarioRepository.existsByUsername(username)) {
            Usuario usuario = Usuario.builder()
                    .id(UUID.randomUUID())
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .nombreCompleto(nombreCompleto)
                    .email(email)
                    .rol(rol)
                    .activo(true)
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            usuarioRepository.save(usuario);
            log.info("[SEGURIDAD] Usuario semilla creado: {} con rol: {}", username, rol);
        }
    }
}
