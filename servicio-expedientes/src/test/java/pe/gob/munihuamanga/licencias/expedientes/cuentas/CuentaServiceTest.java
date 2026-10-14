package pe.gob.munihuamanga.licencias.expedientes.cuentas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarPermisosOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CuentaResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.OperadorCuentaDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.AuditoriaCuentaPermiso;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.AuditoriaCuentaPermisoRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.CuentaService;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private OperadorCuentaRepository operadorCuentaRepository;

    @Mock
    private AuditoriaCuentaPermisoRepository auditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CuentaService cuentaService;

    @BeforeEach
    void setUp() {
        cuentaService = new CuentaService(
                cuentaRepository,
                operadorCuentaRepository,
                auditoriaRepository,
                usuarioRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("I03: Registro de cuenta natural por DNI genera cuenta, usuario y hash de contraseña")
    void registrarCuentaNatural_Exitoso() {
        RegistroCuentaDto dto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("42567891")
                .razonSocialONombre("Juan Quispe")
                .email("juan.quispe@gmail.com")
                .password("PasswordSeguro123!")
                .build();

        when(cuentaRepository.existsByNumeroDocumento("42567891")).thenReturn(false);
        when(cuentaRepository.existsByEmail("juan.quispe@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("PasswordSeguro123!")).thenReturn("$2a$10$hashedPasswordSample");
        when(usuarioRepository.findByUsername("42567891")).thenReturn(Optional.empty());
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaResponseDto resultado = cuentaService.registrarCuenta(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNumeroDocumento()).isEqualTo("42567891");
        assertThat(resultado.getTipoPersona()).isEqualTo(TipoPersona.NATURAL);
        assertThat(resultado.getEmailVerificado()).isFalse();
        assertThat(resultado.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE_VERIFICACION);
        assertThat(resultado.getIdentidadValidadaOficialmente()).isFalse();
        assertThat(resultado.getAvisoIdentidad()).contains("declarativo");

        verify(passwordEncoder).encode("PasswordSeguro123!");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("I03: Registro de cuenta jurídica por RUC designa al Representante Legal como operador con poderes plenos")
    void registrarCuentaJuridica_DesignaRepresentanteLegal() {
        RegistroCuentaDto dto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .numeroDocumento("20608765432")
                .razonSocialONombre("Inversiones Ayacucho S.A.C.")
                .email("contacto@inversionesayacucho.pe")
                .password("PasswordEmpresa123!")
                .nombreRepresentanteLegal("Carlos Mendoza")
                .dniRepresentanteLegal("10293847")
                .build();

        when(cuentaRepository.existsByNumeroDocumento("20608765432")).thenReturn(false);
        when(cuentaRepository.existsByEmail("contacto@inversionesayacucho.pe")).thenReturn(false);
        when(passwordEncoder.encode("PasswordEmpresa123!")).thenReturn("$2a$10$hashedEmpresa");
        when(usuarioRepository.findByUsername("20608765432")).thenReturn(Optional.empty());
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaResponseDto resultado = cuentaService.registrarCuenta(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipoPersona()).isEqualTo(TipoPersona.JURIDICA);
        assertThat(resultado.getOperadores()).hasSize(1);
        OperadorCuentaDto rep = resultado.getOperadores().get(0);
        assertThat(rep.getDniOperador()).isEqualTo("10293847");
        assertThat(rep.getRolOperador()).isEqualTo(RolOperador.REPRESENTANTE_LEGAL);
        assertThat(rep.getPermisos()).contains(PermisoOperador.ADMINISTRAR_OPERADORES, PermisoOperador.CREAR_EXPEDIENTE);
    }

    @Test
    @DisplayName("I03: Rechazo de DNI/RUC duplicado en base de datos")
    void registrarCuenta_DocumentoDuplicado_LanzaExcepcion() {
        RegistroCuentaDto dto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("42567891")
                .razonSocialONombre("Juan Quispe")
                .email("otro.correo@test.pe")
                .password("Pass123!")
                .build();

        when(cuentaRepository.existsByNumeroDocumento("42567891")).thenReturn(true);

        assertThatThrownBy(() -> cuentaService.registrarCuenta(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra registrado");
    }

    @Test
    @DisplayName("I03: Verificación de correo electrónico con token válido activa la cuenta")
    void verificarEmail_TokenValido_ActivaCuenta() {
        String token = UUID.randomUUID().toString();
        Cuenta cuenta = Cuenta.builder()
                .id(UUID.randomUUID())
                .email("test@munihuamanga.gob.pe")
                .tokenVerificacionEmail(token)
                .fechaExpiracionToken(LocalDateTime.now().plusHours(12))
                .emailVerificado(false)
                .estado(EstadoCuenta.PENDIENTE_VERIFICACION)
                .build();

        when(cuentaRepository.findByTokenVerificacionEmail(token)).thenReturn(Optional.of(cuenta));

        boolean resultado = cuentaService.verificarEmail(token);

        assertThat(resultado).isTrue();
        assertThat(cuenta.getEmailVerificado()).isTrue();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(cuenta.getTokenVerificacionEmail()).isNull();
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    @DisplayName("I03: Delegación de operador nominativo y auditoría de permisos")
    void agregarOperador_RegistraAuditoria() {
        UUID cuentaId = UUID.randomUUID();
        Cuenta cuenta = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(TipoPersona.JURIDICA)
                .numeroDocumento("20601234567")
                .razonSocialONombre("Empresa Modelo S.A.")
                .operadores(new ArrayList<>())
                .build();

        CrearOperadorDto dto = CrearOperadorDto.builder()
                .dniOperador("44556677")
                .nombreCompleto("Ana López Tramitadora")
                .email("ana.lopez@modelo.pe")
                .rolOperador(RolOperador.TRAMITADOR)
                .permisos(Set.of(PermisoOperador.CREAR_EXPEDIENTE, PermisoOperador.CONSULTAR_EXPEDIENTE))
                .build();

        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(operadorCuentaRepository.existsByCuentaIdAndDniOperador(cuentaId, "44556677")).thenReturn(false);
        when(usuarioRepository.findByUsername("44556677")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$encodedOp");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(operadorCuentaRepository.save(any(OperadorCuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        OperadorCuentaDto operadorCreado = cuentaService.agregarOperador(cuentaId, dto, "admin_titular");

        assertThat(operadorCreado).isNotNull();
        assertThat(operadorCreado.getDniOperador()).isEqualTo("44556677");
        assertThat(operadorCreado.getRolOperador()).isEqualTo(RolOperador.TRAMITADOR);
        assertThat(operadorCreado.getPermisos()).containsExactlyInAnyOrder(PermisoOperador.CREAR_EXPEDIENTE, PermisoOperador.CONSULTAR_EXPEDIENTE);

        verify(auditoriaRepository).save(any(AuditoriaCuentaPermiso.class));
    }
}
