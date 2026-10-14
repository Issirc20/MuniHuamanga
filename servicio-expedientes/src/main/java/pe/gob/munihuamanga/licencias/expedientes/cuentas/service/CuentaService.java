package pe.gob.munihuamanga.licencias.expedientes.cuentas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.AuditoriaCuentaPermiso;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.AuditoriaCuentaPermisoRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de Negocio para el Módulo M01: Cuentas y Operadores.
 * Aplica validaciones de unicidad de documento, seguridad de contraseñas (BCrypt),
 * verificación de correo y no afirmación de identidad oficial no integrada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final OperadorCuentaRepository operadorCuentaRepository;
    private final AuditoriaCuentaPermisoRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String AVISO_IDENTIDAD_DECLARATIVA =
            "Validación de identidad registrada en modo declarativo institucional. "
            + "Pendiente de homologación e integración con los servicios oficiales de RENIEC y SUNAT (I08).";

    @Transactional
    public CuentaResponseDto registrarCuenta(RegistroCuentaDto dto) {
        String docNormalizado = normalizarDocumento(dto.getNumeroDocumento());
        String emailNormalizado = dto.getEmail().trim().toLowerCase();

        // 1. Validar unicidad estricta de documento de identidad
        if (cuentaRepository.existsByNumeroDocumento(docNormalizado)) {
            log.warn("[CUENTAS] Intento de registro con documento duplicado: {}", docNormalizado);
            throw new IllegalStateException("El documento de identidad " + docNormalizado + " ya se encuentra registrado en una cuenta del sistema municipal.");
        }

        // 2. Validar unicidad de correo electrónico
        if (cuentaRepository.existsByEmail(emailNormalizado)) {
            log.warn("[CUENTAS] Intento de registro con correo electrónico duplicado: {}", emailNormalizado);
            throw new IllegalStateException("El correo electrónico " + emailNormalizado + " ya se encuentra en uso por otra cuenta.");
        }

        UUID cuentaId = UUID.randomUUID();
        String tokenEmail = UUID.randomUUID().toString();

        // 3. Crear entidad Cuenta
        Cuenta cuenta = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(dto.getTipoPersona())
                .tipoDocumento(dto.getTipoDocumento())
                .numeroDocumento(docNormalizado)
                .razonSocialONombre(dto.getRazonSocialONombre().trim())
                .email(emailNormalizado)
                .telefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null)
                .direccionFiscal(dto.getDireccionFiscal() != null ? dto.getDireccionFiscal().trim() : null)
                .emailVerificado(false)
                .tokenVerificacionEmail(tokenEmail)
                .fechaExpiracionToken(LocalDateTime.now().plusHours(24))
                .identidadValidadaOficialmente(false)
                .estado(EstadoCuenta.PENDIENTE_VERIFICACION)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .operadores(new ArrayList<>())
                .establecimientos(new ArrayList<>())
                .build();

        // 4. Crear o actualizar Usuario de autenticación para el titular
        Usuario usuarioTitular = usuarioRepository.findByUsername(docNormalizado)
                .orElseGet(() -> Usuario.builder()
                        .id(cuentaId) // Comparten el mismo ID para trazabilidad directa en login
                        .username(docNormalizado)
                        .nombreCompleto(dto.getRazonSocialONombre().trim())
                        .email(emailNormalizado)
                        .rol(RolUsuario.ROLE_CIUDADANO)
                        .activo(true)
                        .fechaCreacion(LocalDateTime.now())
                        .build());

        usuarioTitular.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuarioTitular.setEmail(emailNormalizado);
        usuarioTitular.setNombreCompleto(dto.getRazonSocialONombre().trim());
        usuarioRepository.save(usuarioTitular);

        // 5. Si es Persona Jurídica, registrar al Representante Legal como primer Operador Nominativo
        if (dto.getTipoPersona() == TipoPersona.JURIDICA && dto.getDniRepresentanteLegal() != null && !dto.getDniRepresentanteLegal().isBlank()) {
            String dniRep = normalizarDocumento(dto.getDniRepresentanteLegal());
            String nombreRep = dto.getNombreRepresentanteLegal() != null ? dto.getNombreRepresentanteLegal().trim() : dto.getRazonSocialONombre().trim();

            OperadorCuenta operadorTitular = OperadorCuenta.builder()
                    .id(UUID.randomUUID())
                    .cuenta(cuenta)
                    .usuario(usuarioTitular)
                    .dniOperador(dniRep)
                    .nombreCompleto(nombreRep)
                    .email(emailNormalizado)
                    .rolOperador(RolOperador.REPRESENTANTE_LEGAL)
                    .permisos(EnumSet.allOf(PermisoOperador.class))
                    .activo(true)
                    .fechaAsignacion(LocalDateTime.now())
                    .asignadoPor("SISTEMA_REGISTRO_TITULAR")
                    .build();

            cuenta.getOperadores().add(operadorTitular);
        }

        Cuenta guardada = cuentaRepository.save(cuenta);
        log.info("[CUENTAS] Nueva cuenta registrada exitosamente: ID={}, Documento={}, Tipo={}",
                guardada.getId(), guardada.getNumeroDocumento(), guardada.getTipoPersona());

        return toDto(guardada);
    }

    @Transactional
    public boolean verificarEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token de verificación de correo no puede estar vacío.");
        }

        Cuenta cuenta = cuentaRepository.findByTokenVerificacionEmail(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de verificación no encontrado o inválido."));

        if (cuenta.getFechaExpiracionToken() != null && LocalDateTime.now().isAfter(cuenta.getFechaExpiracionToken())) {
            throw new IllegalStateException("El token de verificación ha expirado. Solicite un nuevo enlace de activación.");
        }

        cuenta.setEmailVerificado(true);
        cuenta.setEstado(EstadoCuenta.ACTIVA);
        cuenta.setTokenVerificacionEmail(null);
        cuenta.setFechaExpiracionToken(null);
        cuenta.setFechaModificacion(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        log.info("[CUENTAS] Correo verificado con éxito para cuenta ID: {}", cuenta.getId());
        return true;
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
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada con documento: " + docNormalizado));
        return toDto(cuenta);
    }

    @Transactional
    public OperadorCuentaDto agregarOperador(UUID cuentaId, CrearOperadorDto dto, String usuarioResponsable) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada con ID: " + cuentaId));

        String dniNormalizado = normalizarDocumento(dto.getDniOperador());

        if (operadorCuentaRepository.existsByCuentaIdAndDniOperador(cuentaId, dniNormalizado)) {
            throw new IllegalStateException("El operador con DNI " + dniNormalizado + " ya está asignado a esta cuenta.");
        }

        // Buscar o crear usuario de acceso para el operador nominativo
        Usuario usuarioOp = usuarioRepository.findByUsername(dniNormalizado)
                .orElseGet(() -> {
                    Usuario nuevo = Usuario.builder()
                            .id(UUID.randomUUID())
                            .username(dniNormalizado)
                            .password(passwordEncoder.encode("Operador" + dniNormalizado + "!")) // Temporal para primer acceso
                            .nombreCompleto(dto.getNombreCompleto().trim())
                            .email(dto.getEmail().trim().toLowerCase())
                            .rol(RolUsuario.ROLE_CIUDADANO)
                            .activo(true)
                            .fechaCreacion(LocalDateTime.now())
                            .build();
                    return usuarioRepository.save(nuevo);
                });

        UUID operadorId = UUID.randomUUID();
        Set<PermisoOperador> permisosAsignados = dto.getPermisos() != null ? new HashSet<>(dto.getPermisos()) : new HashSet<>();

        OperadorCuenta operador = OperadorCuenta.builder()
                .id(operadorId)
                .cuenta(cuenta)
                .usuario(usuarioOp)
                .dniOperador(dniNormalizado)
                .nombreCompleto(dto.getNombreCompleto().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .rolOperador(dto.getRolOperador())
                .permisos(permisosAsignados)
                .activo(true)
                .fechaAsignacion(LocalDateTime.now())
                .asignadoPor(usuarioResponsable)
                .build();

        OperadorCuenta guardado = operadorCuentaRepository.save(operador);

        // Registro de auditoría
        auditoriaRepository.save(AuditoriaCuentaPermiso.builder()
                .id(UUID.randomUUID())
                .cuentaId(cuentaId)
                .operadorId(operadorId)
                .accion("ASIGNAR_OPERADOR")
                .permisosAnteriores(null)
                .permisosNuevos(permisosAsignados.stream().map(Enum::name).collect(Collectors.joining(",")))
                .usuarioResponsable(usuarioResponsable)
                .fecha(LocalDateTime.now())
                .build());

        log.info("[CUENTAS] Operador asignado: ID={}, DNI={}, Cuenta={}, Rol={}, AsignadoPor={}",
                operadorId, dniNormalizado, cuentaId, dto.getRolOperador(), usuarioResponsable);

        return toOperadorDto(guardado);
    }

    @Transactional
    public OperadorCuentaDto actualizarPermisosOperador(UUID cuentaId, UUID operadorId, ActualizarPermisosOperadorDto dto, String usuarioResponsable) {
        OperadorCuenta operador = operadorCuentaRepository.findById(operadorId)
                .orElseThrow(() -> new NoSuchElementException("Operador no encontrado con ID: " + operadorId));

        if (!operador.getCuenta().getId().equals(cuentaId)) {
            throw new IllegalArgumentException("El operador no pertenece a la cuenta especificada.");
        }

        String permisosAnteriores = operador.getPermisos().stream().map(Enum::name).collect(Collectors.joining(","));

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
        String permisosNuevos = actualizado.getPermisos().stream().map(Enum::name).collect(Collectors.joining(","));

        // Registro de auditoría
        auditoriaRepository.save(AuditoriaCuentaPermiso.builder()
                .id(UUID.randomUUID())
                .cuentaId(cuentaId)
                .operadorId(operadorId)
                .accion("MODIFICAR_PERMISOS")
                .permisosAnteriores(permisosAnteriores)
                .permisosNuevos(permisosNuevos)
                .usuarioResponsable(usuarioResponsable)
                .fecha(LocalDateTime.now())
                .build());

        log.info("[CUENTAS] Permisos de operador actualizados: ID={}, Cuenta={}, Responsable={}",
                operadorId, cuentaId, usuarioResponsable);

        return toOperadorDto(actualizado);
    }

    @Transactional(readOnly = true)
    public List<OperadorCuentaDto> listarOperadores(UUID cuentaId) {
        return operadorCuentaRepository.findByCuentaId(cuentaId).stream()
                .map(this::toOperadorDto)
                .collect(Collectors.toList());
    }

    private String normalizarDocumento(String documento) {
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
