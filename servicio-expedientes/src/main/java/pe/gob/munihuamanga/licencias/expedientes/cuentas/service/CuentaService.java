package pe.gob.munihuamanga.licencias.expedientes.cuentas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarPermisosOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CuentaResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.EstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.OperadorCuentaDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.AuditoriaCuentaPermiso;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.AuditoriaCuentaPermisoRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.GeneradorTokensSeguros;
import pe.gob.munihuamanga.licencias.expedientes.security.PoliticaContrasenas;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Registro de cuentas de administrado y gestión de sus representantes.
 * Las credenciales pertenecen siempre a personas naturales: el titular de una cuenta de persona natural
 * o el representante legal de una persona jurídica. El vínculo persona-cuenta es una representación explícita.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaService {

    public static final String AVISO_IDENTIDAD_DECLARATIVA =
            "La identidad registrada es declarativa hasta su validación por mesa de partes.";
    static final String MENSAJE_REGISTRO_NO_DISPONIBLE =
            "No es posible completar el registro con los datos proporcionados. Si ya cuenta con acceso, utilice la opción de inicio de sesión.";
    private static final long HORAS_VIGENCIA_VERIFICACION = 24;

    private final CuentaRepository cuentaRepository;
    private final OperadorCuentaRepository operadorCuentaRepository;
    private final AuditoriaCuentaPermisoRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasenas politicaContrasenas;
    private final PuertoNotificacionCuenta notificacionCuenta;
    private final ActivacionAccesoService activacionAccesoService;
    private final ProveedorActor proveedorActor;
    private final Clock reloj;

    @Value("${notificacion.email.portal-ciudadano:http://localhost:8081}")
    private String urlPortal;

    @Transactional
    public CuentaResponseDto registrarCuenta(RegistroCuentaDto dto) {
        String documento = normalizarDocumento(dto.getNumeroDocumento());
        String email = dto.getEmail().trim().toLowerCase();
        boolean juridica = dto.getTipoPersona() == TipoPersona.JURIDICA;

        if (cuentaRepository.existsByNumeroDocumento(documento) || cuentaRepository.existsByEmail(email)) {
            log.warn("[CUENTAS] Registro rechazado por datos ya registrados.");
            throw new IllegalStateException(MENSAJE_REGISTRO_NO_DISPONIBLE);
        }

        String dniPersona = juridica ? normalizarDocumento(dto.getDniRepresentanteLegal()) : documento;
        String nombrePersona = juridica ? textoObligatorio(dto.getNombreRepresentanteLegal(),
                "El nombre del representante legal es obligatorio para personas jurídicas.") : dto.getRazonSocialONombre().trim();
        if (juridica && dniPersona.length() != 8) {
            throw new IllegalArgumentException("El DNI del representante legal es obligatorio para personas jurídicas.");
        }

        Usuario persona = obtenerOCrearPersona(dniPersona, nombrePersona, email, dto.getPassword());

        LocalDateTime ahora = LocalDateTime.now(reloj);
        String tokenVerificacion = GeneradorTokensSeguros.nuevoToken();
        Cuenta cuenta = Cuenta.builder()
                .id(UUID.randomUUID())
                .tipoPersona(dto.getTipoPersona())
                .tipoDocumento(dto.getTipoDocumento())
                .numeroDocumento(documento)
                .razonSocialONombre(dto.getRazonSocialONombre().trim())
                .email(email)
                .telefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null)
                .direccionFiscal(dto.getDireccionFiscal() != null ? dto.getDireccionFiscal().trim() : null)
                .emailVerificado(false)
                .tokenVerificacionEmail(GeneradorTokensSeguros.resumen(tokenVerificacion))
                .fechaExpiracionToken(ahora.plusHours(HORAS_VIGENCIA_VERIFICACION))
                .identidadValidadaOficialmente(false)
                .estado(EstadoCuenta.PENDIENTE_VERIFICACION)
                .activo(true)
                .fechaCreacion(ahora)
                .operadores(new ArrayList<>())
                .establecimientos(new ArrayList<>())
                .build();

        cuenta.getOperadores().add(OperadorCuenta.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .usuario(persona)
                .dniOperador(dniPersona)
                .nombreCompleto(nombrePersona)
                .email(email)
                .rolOperador(juridica ? RolOperador.REPRESENTANTE_LEGAL : RolOperador.TITULAR)
                .permisos(EnumSet.allOf(PermisoOperador.class))
                .activo(true)
                .fechaAsignacion(ahora)
                .asignadoPor("REGISTRO_EN_PORTAL")
                .build());

        Cuenta guardada = cuentaRepository.save(cuenta);
        notificacionCuenta.enviarVerificacionCorreo(email, cuenta.getRazonSocialONombre(),
                urlPortal + "/verificar-correo.html?token=" + tokenVerificacion);
        log.info("[CUENTAS] Cuenta {} registrada ({}).", guardada.getId(), guardada.getTipoPersona());
        return toDto(guardada);
    }

    @Transactional
    public boolean verificarEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El enlace de verificación no es válido.");
        }
        Cuenta cuenta = cuentaRepository.findByTokenVerificacionEmail(GeneradorTokensSeguros.resumen(token))
                .orElseThrow(() -> new IllegalArgumentException("El enlace de verificación no es válido o ya fue utilizado."));
        if (cuenta.getFechaExpiracionToken() != null && LocalDateTime.now(reloj).isAfter(cuenta.getFechaExpiracionToken())) {
            throw new IllegalStateException("El enlace de verificación ha vencido. Solicite uno nuevo desde el portal.");
        }
        cuenta.setEmailVerificado(true);
        cuenta.setEstado(EstadoCuenta.ACTIVA);
        cuenta.setTokenVerificacionEmail(null);
        cuenta.setFechaExpiracionToken(null);
        cuenta.setFechaModificacion(LocalDateTime.now(reloj));
        cuentaRepository.save(cuenta);
        log.info("[CUENTAS] Correo verificado para la cuenta {}.", cuenta.getId());
        return true;
    }

    /**
     * Reenvía el enlace de verificación. La respuesta es la misma exista o no la cuenta,
     * para no revelar qué correos están registrados.
     */
    @Transactional
    public void reenviarVerificacion(String email) {
        Optional<Cuenta> cuenta = cuentaRepository.findByEmail(email.trim().toLowerCase())
                .filter(c -> !Boolean.TRUE.equals(c.getEmailVerificado()));
        cuenta.ifPresent(c -> {
            String token = GeneradorTokensSeguros.nuevoToken();
            c.setTokenVerificacionEmail(GeneradorTokensSeguros.resumen(token));
            c.setFechaExpiracionToken(LocalDateTime.now(reloj).plusHours(HORAS_VIGENCIA_VERIFICACION));
            cuentaRepository.save(c);
            notificacionCuenta.enviarVerificacionCorreo(c.getEmail(), c.getRazonSocialONombre(),
                    urlPortal + "/verificar-correo.html?token=" + token);
        });
    }

    @Transactional(readOnly = true)
    public CuentaResponseDto obtenerPorId(UUID cuentaId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada con identificador: " + cuentaId));
        return toDto(cuenta);
    }

    @Transactional(readOnly = true)
    public CuentaResponseDto obtenerPorDocumento(String numeroDocumento) {
        String docNormalizado = normalizarDocumento(numeroDocumento);
        Cuenta cuenta = cuentaRepository.findByNumeroDocumento(docNormalizado)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada con el documento indicado."));
        return toDto(cuenta);
    }

    /**
     * Asigna un representante a la cuenta. Si la persona aún no tiene acceso, se le envía un enlace
     * de activación para que defina su propia contraseña.
     */
    @Transactional
    public OperadorCuentaDto agregarOperador(UUID cuentaId, CrearOperadorDto dto) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada con ID: " + cuentaId));
        String dni = normalizarDocumento(dto.getDniOperador());
        if (operadorCuentaRepository.existsByCuentaIdAndDniOperador(cuentaId, dni)) {
            throw new IllegalStateException("La persona indicada ya es representante de esta cuenta.");
        }

        String email = dto.getEmail().trim().toLowerCase();
        Usuario persona = usuarioRepository.findByUsername(dni).orElseGet(() -> {
            Usuario nueva = usuarioRepository.save(Usuario.builder()
                    .id(UUID.randomUUID())
                    .username(dni)
                    .password(passwordEncoder.encode(GeneradorTokensSeguros.nuevoToken()))
                    .nombreCompleto(dto.getNombreCompleto().trim())
                    .email(email)
                    .roles(new HashSet<>(Set.of(RolUsuario.ROLE_CIUDADANO)))
                    .activo(true)
                    .fechaCreacion(LocalDateTime.now(reloj))
                    .build());
            activacionAccesoService.solicitarActivacion(nueva);
            return nueva;
        });

        String responsable = proveedorActor.actual().nombreParaRegistro();
        Set<PermisoOperador> permisos = dto.getPermisos() != null ? new HashSet<>(dto.getPermisos()) : new HashSet<>();
        OperadorCuenta guardado = operadorCuentaRepository.save(OperadorCuenta.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .usuario(persona)
                .dniOperador(dni)
                .nombreCompleto(dto.getNombreCompleto().trim())
                .email(email)
                .rolOperador(dto.getRolOperador())
                .permisos(permisos)
                .activo(true)
                .fechaAsignacion(LocalDateTime.now(reloj))
                .asignadoPor(responsable)
                .build());

        registrarAuditoria(cuentaId, guardado.getId(), "ASIGNAR_OPERADOR", null, permisos, responsable);
        log.info("[CUENTAS] Representante {} asignado a la cuenta {} con rol {}.", guardado.getId(), cuentaId, dto.getRolOperador());
        return toOperadorDto(guardado);
    }

    @Transactional
    public OperadorCuentaDto actualizarPermisosOperador(UUID cuentaId, UUID operadorId, ActualizarPermisosOperadorDto dto) {
        OperadorCuenta operador = operadorCuentaRepository.findById(operadorId)
                .orElseThrow(() -> new NoSuchElementException("Representante no encontrado con ID: " + operadorId));
        if (!operador.getCuenta().getId().equals(cuentaId)) {
            throw new IllegalArgumentException("El representante no pertenece a la cuenta especificada.");
        }
        if (operador.getRolOperador() == RolOperador.TITULAR) {
            throw new IllegalStateException("Las facultades del titular de la cuenta no pueden modificarse.");
        }

        Set<PermisoOperador> anteriores = new HashSet<>(operador.getPermisos());
        if (dto.getRolOperador() != null) {
            operador.setRolOperador(dto.getRolOperador());
        }
        if (dto.getPermisos() != null) {
            operador.setPermisos(new HashSet<>(dto.getPermisos()));
        }
        if (dto.getActivo() != null) {
            operador.setActivo(dto.getActivo());
        }
        OperadorCuenta actualizado = operadorCuentaRepository.save(operador);

        String responsable = proveedorActor.actual().nombreParaRegistro();
        registrarAuditoria(cuentaId, operadorId, "MODIFICAR_PERMISOS", anteriores, actualizado.getPermisos(), responsable);
        return toOperadorDto(actualizado);
    }

    @Transactional(readOnly = true)
    public List<OperadorCuentaDto> listarOperadores(UUID cuentaId) {
        return operadorCuentaRepository.findByCuentaId(cuentaId).stream()
                .map(this::toOperadorDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la persona con acceso para el documento indicado o la crea. Si ya existe, la contraseña
     * proporcionada debe coincidir con la registrada: así se acredita que quien registra es esa persona.
     */
    private Usuario obtenerOCrearPersona(String dni, String nombre, String email, String password) {
        Optional<Usuario> existente = usuarioRepository.findByUsername(dni);
        if (existente.isPresent()) {
            if (!passwordEncoder.matches(password, existente.get().getPassword())) {
                throw new IllegalStateException(MENSAJE_REGISTRO_NO_DISPONIBLE);
            }
            return existente.get();
        }
        politicaContrasenas.exigir(password, dni);
        return usuarioRepository.save(Usuario.builder()
                .id(UUID.randomUUID())
                .username(dni)
                .password(passwordEncoder.encode(password))
                .nombreCompleto(nombre)
                .email(email)
                .roles(new HashSet<>(Set.of(RolUsuario.ROLE_CIUDADANO)))
                .activo(true)
                .fechaCreacion(LocalDateTime.now(reloj))
                .build());
    }

    private void registrarAuditoria(UUID cuentaId, UUID operadorId, String accion,
                                    Set<PermisoOperador> anteriores, Set<PermisoOperador> nuevos, String responsable) {
        auditoriaRepository.save(AuditoriaCuentaPermiso.builder()
                .id(UUID.randomUUID())
                .cuentaId(cuentaId)
                .operadorId(operadorId)
                .accion(accion)
                .permisosAnteriores(anteriores != null ? anteriores.stream().map(Enum::name).collect(Collectors.joining(",")) : null)
                .permisosNuevos(nuevos.stream().map(Enum::name).collect(Collectors.joining(",")))
                .usuarioResponsable(responsable)
                .fecha(LocalDateTime.now(reloj))
                .build());
    }

    private static String textoObligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }

    private static String normalizarDocumento(String documento) {
        if (documento == null) return "";
        return documento.trim().toUpperCase().replaceAll("[^0-9A-Z]", "");
    }

    public CuentaResponseDto toDto(Cuenta cuenta) {
        return CuentaResponseDto.builder()
                .id(cuenta.getId())
                .tipoPersona(cuenta.getTipoPersona())
                .tipoDocumento(cuenta.getTipoDocumento())
                .numeroDocumento(cuenta.getNumeroDocumento())
                .razonSocialONombre(cuenta.getRazonSocialONombre())
                .email(cuenta.getEmail())
                .telefono(cuenta.getTelefono())
                .direccionFiscal(cuenta.getDireccionFiscal())
                .emailVerificado(cuenta.getEmailVerificado())
                .estado(cuenta.getEstado())
                .identidadValidadaOficialmente(cuenta.getIdentidadValidadaOficialmente())
                .avisoIdentidad(AVISO_IDENTIDAD_DECLARATIVA)
                .activo(cuenta.getActivo())
                .fechaCreacion(cuenta.getFechaCreacion())
                .operadores(cuenta.getOperadores() != null ? cuenta.getOperadores().stream().map(this::toOperadorDto).collect(Collectors.toList()) : new ArrayList<>())
                .establecimientos(cuenta.getEstablecimientos() != null ? cuenta.getEstablecimientos().stream().map(this::toEstablecimientoDto).collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    public OperadorCuentaDto toOperadorDto(OperadorCuenta operador) {
        return OperadorCuentaDto.builder()
                .id(operador.getId())
                .cuentaId(operador.getCuenta().getId())
                .usuarioId(operador.getUsuario() != null ? operador.getUsuario().getId() : null)
                .dniOperador(operador.getDniOperador())
                .nombreCompleto(operador.getNombreCompleto())
                .email(operador.getEmail())
                .rolOperador(operador.getRolOperador())
                .permisos(new HashSet<>(operador.getPermisos()))
                .activo(operador.getActivo())
                .fechaAsignacion(operador.getFechaAsignacion())
                .asignadoPor(operador.getAsignadoPor())
                .build();
    }

    private EstablecimientoDto toEstablecimientoDto(Establecimiento est) {
        return EstablecimientoDto.builder()
                .id(est.getId())
                .cuentaId(est.getCuenta().getId())
                .codigoEstablecimiento(est.getCodigoEstablecimiento())
                .nombreComercial(est.getNombreComercial())
                .direccion(est.getDireccion())
                .distrito(est.getDistrito())
                .provincia(est.getProvincia())
                .departamento(est.getDepartamento())
                .referencia(est.getReferencia())
                .latitud(est.getLatitud())
                .longitud(est.getLongitud())
                .ciiuCodigo(est.getCiiu() != null ? est.getCiiu().getCodigo() : null)
                .giroPrincipal(est.getGiroPrincipal())
                .areaMetrosCuadrados(est.getAreaMetrosCuadrados())
                .advertenciaZonificacion("La ubicación declarada es referencial y no constituye zonificación municipal aprobada.")
                .activo(est.getActivo())
                .fechaRegistro(est.getFechaRegistro())
                .fechaModificacion(est.getFechaModificacion())
                .build();
    }
}
