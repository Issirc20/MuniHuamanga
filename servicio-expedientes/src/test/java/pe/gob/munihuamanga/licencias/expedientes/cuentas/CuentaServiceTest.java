package pe.gob.munihuamanga.licencias.expedientes.cuentas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.gob.munihuamanga.licencias.common.dto.CrearOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CuentaResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.AuditoriaCuentaPermisoRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.ActivacionAccesoService;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.CuentaService;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.PuertoNotificacionCuenta;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.GeneradorTokensSeguros;
import pe.gob.munihuamanga.licencias.expedientes.security.PoliticaContrasenasInstitucional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    private static final String CONTRASENA_VALIDA = "Registro.Seguro26";

    @Mock private CuentaRepository cuentaRepository;
    @Mock private OperadorCuentaRepository operadorCuentaRepository;
    @Mock private AuditoriaCuentaPermisoRepository auditoriaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PuertoNotificacionCuenta notificacionCuenta;
    @Mock private ActivacionAccesoService activacionAccesoService;
    @Mock private ProveedorActor proveedorActor;

    private CuentaService cuentaService;
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-30T15:00:00Z"), ZoneId.of("America/Lima"));

    @BeforeEach
    void setUp() {
        cuentaService = new CuentaService(cuentaRepository, operadorCuentaRepository, auditoriaRepository,
                usuarioRepository, passwordEncoder, new PoliticaContrasenasInstitucional(), notificacionCuenta,
                activacionAccesoService, proveedorActor, reloj);
    }

    @Test
    @DisplayName("El registro de persona natural crea la persona, la cuenta y la representación de titular, y envía la verificación")
    void registroPersonaNatural() {
        when(cuentaRepository.existsByNumeroDocumento("42567891")).thenReturn(false);
        when(cuentaRepository.existsByEmail("juan.quispe@correo.pe")).thenReturn(false);
        when(usuarioRepository.findByUsername("42567891")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(CONTRASENA_VALIDA)).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaResponseDto resultado = cuentaService.registrarCuenta(registro(TipoPersona.NATURAL, "42567891", null));

        ArgumentCaptor<Cuenta> cuenta = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(cuenta.capture());
        OperadorCuenta titular = cuenta.getValue().getOperadores().get(0);
        assertThat(titular.getRolOperador()).isEqualTo(RolOperador.TITULAR);
        assertThat(titular.getPermisos()).contains(PermisoOperador.PRESENTAR_EXPEDIENTE);
        assertThat(titular.getUsuario().getId()).isNotEqualTo(cuenta.getValue().getId());
        assertThat(resultado.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE_VERIFICACION);
        verify(notificacionCuenta).enviarVerificacionCorreo(eq("juan.quispe@correo.pe"), anyString(), anyString());
    }

    @Test
    @DisplayName("En persona jurídica las credenciales pertenecen al representante legal, no al RUC")
    void registroPersonaJuridica() {
        when(cuentaRepository.existsByNumeroDocumento("20608765432")).thenReturn(false);
        when(cuentaRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.findByUsername("10293847")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(CONTRASENA_VALIDA)).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        cuentaService.registrarCuenta(registro(TipoPersona.JURIDICA, "20608765432", "10293847"));

        ArgumentCaptor<Usuario> persona = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(persona.capture());
        assertThat(persona.getValue().getUsername()).isEqualTo("10293847");
        verify(usuarioRepository, never()).findByUsername("20608765432");
    }

    @Test
    @DisplayName("El registro con documento ya registrado devuelve un mensaje neutro")
    void registroDuplicado() {
        when(cuentaRepository.existsByNumeroDocumento("42567891")).thenReturn(true);

        assertThatThrownBy(() -> cuentaService.registrarCuenta(registro(TipoPersona.NATURAL, "42567891", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("42567891");
    }

    @Test
    @DisplayName("Si la persona ya tiene acceso, el registro exige su contraseña actual y no la sobrescribe")
    void registroConPersonaExistenteYContrasenaDistinta() {
        Usuario existente = Usuario.builder().id(UUID.randomUUID()).username("10293847").password("hash-actual").build();
        when(cuentaRepository.existsByNumeroDocumento(anyString())).thenReturn(false);
        when(cuentaRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.findByUsername("10293847")).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches(CONTRASENA_VALIDA, "hash-actual")).thenReturn(false);

        assertThatThrownBy(() -> cuentaService.registrarCuenta(registro(TipoPersona.JURIDICA, "20608765432", "10293847")))
                .isInstanceOf(IllegalStateException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("La contraseña debe cumplir la política institucional")
    void contrasenaDebil() {
        when(cuentaRepository.existsByNumeroDocumento(anyString())).thenReturn(false);
        when(cuentaRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.findByUsername("42567891")).thenReturn(Optional.empty());
        RegistroCuentaDto dto = registro(TipoPersona.NATURAL, "42567891", null);
        dto.setPassword("sinmayusculas1");

        assertThatThrownBy(() -> cuentaService.registrarCuenta(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("La verificación de correo se realiza con el resumen del token y activa la cuenta")
    void verificacionDeCorreo() {
        String token = "token-de-verificacion";
        Cuenta cuenta = Cuenta.builder().id(UUID.randomUUID()).estado(EstadoCuenta.PENDIENTE_VERIFICACION)
                .fechaExpiracionToken(LocalDateTime.now(reloj).plusHours(1)).build();
        when(cuentaRepository.findByTokenVerificacionEmail(GeneradorTokensSeguros.resumen(token))).thenReturn(Optional.of(cuenta));

        assertThat(cuentaService.verificarEmail(token)).isTrue();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(cuenta.getTokenVerificacionEmail()).isNull();
    }

    @Test
    @DisplayName("Un representante sin acceso previo recibe un enlace de activación; no se genera contraseña predecible")
    void agregarOperadorNuevo() {
        Cuenta cuenta = Cuenta.builder().id(UUID.randomUUID()).operadores(new ArrayList<>()).build();
        when(cuentaRepository.findById(cuenta.getId())).thenReturn(Optional.of(cuenta));
        when(operadorCuentaRepository.existsByCuentaIdAndDniOperador(cuenta.getId(), "41223344")).thenReturn(false);
        when(usuarioRepository.findByUsername("41223344")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-aleatorio");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(operadorCuentaRepository.save(any(OperadorCuenta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proveedorActor.actual()).thenReturn(Actor.sistema());

        cuentaService.agregarOperador(cuenta.getId(), CrearOperadorDto.builder()
                .dniOperador("41223344")
                .nombreCompleto("Ana Tramitadora")
                .email("ana@correo.pe")
                .rolOperador(RolOperador.TRAMITADOR)
                .build());

        verify(passwordEncoder, never()).encode("Operador41223344!");
        verify(activacionAccesoService).solicitarActivacion(any(Usuario.class));
    }

    private static RegistroCuentaDto registro(TipoPersona tipo, String documento, String dniRepresentante) {
        return RegistroCuentaDto.builder()
                .tipoPersona(tipo)
                .tipoDocumento(tipo == TipoPersona.NATURAL ? TipoDocumento.DNI : TipoDocumento.RUC)
                .numeroDocumento(documento)
                .razonSocialONombre(tipo == TipoPersona.NATURAL ? "Juan Quispe" : "Inversiones Ayacucho S.A.C.")
                .email(tipo == TipoPersona.NATURAL ? "juan.quispe@correo.pe" : "contacto@inversiones.pe")
                .password(CONTRASENA_VALIDA)
                .dniRepresentanteLegal(dniRepresentante)
                .nombreRepresentanteLegal(dniRepresentante != null ? "Carlos Mendoza" : null)
                .build();
    }
}
