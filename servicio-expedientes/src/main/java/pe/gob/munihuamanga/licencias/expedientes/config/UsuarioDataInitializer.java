package pe.gob.munihuamanga.licencias.expedientes.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Inicializador de usuarios y cuentas semilla para desarrollo y pruebas del sistema municipal.
 * Excluido del entorno de producción ('prod') por razones de seguridad (H02).
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class UsuarioDataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
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
        crearCuentaSiNoExiste(
                UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
                TipoPersona.NATURAL,
                TipoDocumento.DNI,
                "42567891",
                "María Quispe Huamán",
                "maria.quispe@gmail.com",
                "966123456",
                "Jr. 9 de Diciembre N° 142, Ayacucho"
        );

        crearUsuarioSiNoExiste(
                UUID.fromString("78999999-0000-0000-0000-000000000001"),
                "78999999",
                "ciud123",
                "Juan Pérez Otro - Administrado No Propietario",
                "juan.perez@gmail.com",
                RolUsuario.ROLE_CIUDADANO
        );
        crearCuentaSiNoExiste(
                UUID.fromString("78999999-0000-0000-0000-000000000001"),
                TipoPersona.NATURAL,
                TipoDocumento.DNI,
                "78999999",
                "Juan Pérez Otro",
                "juan.perez@gmail.com",
                "966777888",
                "Jr. Manco Cápac N° 201, Ayacucho"
        );
    }

    private void crearCuentaSiNoExiste(
            UUID id,
            TipoPersona tipoPersona,
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            String razonSocialONombre,
            String email,
            String telefono,
            String direccionFiscal
    ) {
        if (!cuentaRepository.existsByNumeroDocumento(numeroDocumento)) {
            Cuenta cuenta = Cuenta.builder()
                    .id(id)
                    .tipoPersona(tipoPersona)
                    .tipoDocumento(tipoDocumento)
                    .numeroDocumento(numeroDocumento)
                    .razonSocialONombre(razonSocialONombre)
                    .email(email)
                    .telefono(telefono)
                    .direccionFiscal(direccionFiscal)
                    .emailVerificado(true)
                    .identidadValidadaOficialmente(true)
                    .estado(EstadoCuenta.ACTIVA)
                    .activo(true)
                    .fechaCreacion(LocalDateTime.now())
                    .operadores(new ArrayList<>())
                    .establecimientos(new ArrayList<>())
                    .build();

            cuentaRepository.save(cuenta);
            log.info("[CUENTAS] Cuenta semilla activa creada para documento: {} (ID: {})", numeroDocumento, id);
        }
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
