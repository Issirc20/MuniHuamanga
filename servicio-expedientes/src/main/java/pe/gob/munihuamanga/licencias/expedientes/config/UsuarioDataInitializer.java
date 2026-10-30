package pe.gob.munihuamanga.licencias.expedientes.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.MedioValidacionIdentidad;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Accesos de prueba para los entornos de desarrollo y demostración: un funcionario por rol institucional
 * y dos administrados de persona natural. No se ejecuta en producción.
 */
@Slf4j
@Component
@Profile("!prod")
@Order(1)
@RequiredArgsConstructor
public class UsuarioDataInitializer implements CommandLineRunner {

    public static final UUID CUENTA_MARIA_QUISPE = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    public static final UUID CUENTA_JUAN_PEREZ = UUID.fromString("78999999-0000-0000-0000-000000000001");

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        crearFuncionario("admin", "Sistemas.Huamanga26", "Administración del Sistema", "sistemas@munihuamanga.gob.pe", RolUsuario.ROLE_ADMIN);
        crearFuncionario("mesapartes", "Ventanilla.Huamanga26", "Mesa de Partes de Licencias", "mesadepartes@munihuamanga.gob.pe", RolUsuario.ROLE_MESA_DE_PARTES);
        crearFuncionario("evaluador", "Licencias.Huamanga26", "Evaluador de Licencias de Funcionamiento", "evaluacion.licencias@munihuamanga.gob.pe", RolUsuario.ROLE_EVALUADOR);
        crearFuncionario("supervisor", "Revision.Huamanga26", "Subgerencia de Comercio y Licencias", "subgerencia.licencias@munihuamanga.gob.pe", RolUsuario.ROLE_SUPERVISOR_LICENCIAS);
        crearFuncionario("autoridad", "Gerencia.Huamanga26", "Gerencia de Desarrollo Económico", "gerencia.desarrollo@munihuamanga.gob.pe", RolUsuario.ROLE_AUTORIDAD_RESOLUTIVA);
        crearFuncionario("cajero", "Caja.Huamanga2026", "Caja del SAT Huamanga", "caja@sat-huamanga.gob.pe", RolUsuario.ROLE_CAJERO);
        crearFuncionario("inspector", "Campo.Huamanga26", "Inspección Municipal", "inspeccion@munihuamanga.gob.pe", RolUsuario.ROLE_INSPECTOR);
        crearFuncionario("auditor", "Control.Huamanga26", "Órgano de Control Institucional", "control@munihuamanga.gob.pe", RolUsuario.ROLE_AUDITOR);

        crearAdministrado(CUENTA_MARIA_QUISPE, "42567891", "María Quispe Huamán", "maria.quispe@correo.pe",
                "966123456", "Jr. 9 de Diciembre N° 142, Ayacucho");
        crearAdministrado(CUENTA_JUAN_PEREZ, "78999999", "Juan Pérez Ramos", "juan.perez@correo.pe",
                "966777888", "Jr. Manco Cápac N° 201, Ayacucho");
    }

    private void crearFuncionario(String username, String contrasena, String nombre, String email, RolUsuario rol) {
        if (usuarioRepository.existsByUsername(username)) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .id(UUID.randomUUID())
                .username(username)
                .password(passwordEncoder.encode(contrasena))
                .nombreCompleto(nombre)
                .email(email)
                .roles(new HashSet<>(Set.of(rol)))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build());
        log.info("[DATOS DE PRUEBA] Funcionario '{}' creado con el rol {}.", username, rol);
    }

    private void crearAdministrado(UUID cuentaId, String dni, String nombre, String email, String telefono, String direccion) {
        if (cuentaRepository.existsByNumeroDocumento(dni)) {
            return;
        }
        Usuario persona = usuarioRepository.findByUsername(dni).orElseGet(() -> usuarioRepository.save(Usuario.builder()
                .id(UUID.randomUUID())
                .username(dni)
                .password(passwordEncoder.encode("Ciudadano.Huamanga26"))
                .nombreCompleto(nombre)
                .email(email)
                .roles(new HashSet<>(Set.of(RolUsuario.ROLE_CIUDADANO)))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build()));

        Cuenta cuenta = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(dni)
                .razonSocialONombre(nombre)
                .email(email)
                .telefono(telefono)
                .direccionFiscal(direccion)
                .emailVerificado(true)
                .identidadValidadaOficialmente(true)
                .medioValidacionIdentidad(MedioValidacionIdentidad.PRESENCIAL_DOCUMENTO_ORIGINAL)
                .referenciaValidacionIdentidad("Datos de prueba")
                .identidadValidadaPor("SISTEMA")
                .fechaValidacionIdentidad(LocalDateTime.now())
                .estado(EstadoCuenta.ACTIVA)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .operadores(new ArrayList<>())
                .establecimientos(new ArrayList<>())
                .build();
        cuenta.getOperadores().add(OperadorCuenta.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .usuario(persona)
                .dniOperador(dni)
                .nombreCompleto(nombre)
                .email(email)
                .rolOperador(RolOperador.TITULAR)
                .permisos(EnumSet.allOf(PermisoOperador.class))
                .activo(true)
                .fechaAsignacion(LocalDateTime.now())
                .asignadoPor("DATOS_DE_PRUEBA")
                .build());
        cuentaRepository.save(cuenta);
        log.info("[DATOS DE PRUEBA] Cuenta de administrado creada para {}.", nombre);
    }
}
