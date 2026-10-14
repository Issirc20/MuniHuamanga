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

        crearUsuarioSiNoExiste(
                "inspector",
                "insp123",
                "Téc. Raúl Cárdenas - Inspector Municipal",
                "raul.cardenas@munihuamanga.gob.pe",
                RolUsuario.ROLE_INSPECTOR
        );

        crearUsuarioSiNoExiste(
                UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
                "42567891",
                "ciud123",
                "María Quispe Huamán - Administrada Titular",
                "maria.quispe@gmail.com",
                RolUsuario.ROLE_CIUDADANO
        );

        crearUsuarioSiNoExiste(
                UUID.fromString("78999999-0000-0000-0000-000000000001"),
                "78999999",
                "ciud123",
                "Juan Pérez Otro - Administrado No Propietario",
                "juan.perez@gmail.com",
                RolUsuario.ROLE_CIUDADANO
        );
    }

    private void crearUsuarioSiNoExiste(
            String username,
            String rawPassword,
            String nombreCompleto,
            String email,
            RolUsuario rol
    ) {
        crearUsuarioSiNoExiste(UUID.randomUUID(), username, rawPassword, nombreCompleto, email, rol);
    }

    private void crearUsuarioSiNoExiste(
            UUID id,
            String username,
            String rawPassword,
            String nombreCompleto,
            String email,
            RolUsuario rol
    ) {
        if (!usuarioRepository.existsByUsername(username)) {
            Usuario usuario = Usuario.builder()
                    .id(id != null ? id : UUID.randomUUID())
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .nombreCompleto(nombreCompleto)
                    .email(email)
                    .rol(rol)
                    .activo(true)
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            usuarioRepository.save(usuario);
            log.info("[SEGURIDAD] Usuario semilla creado: {} ({}) con rol: {}", username, usuario.getId(), rol);
        }
    }
}
