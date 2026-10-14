package pe.gob.munihuamanga.licencias.expedientes.zonificacion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.EvaluacionZonificacionDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistrarEvaluacionZonificacionDto;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.borradores.security.DocumentoSecurityValidator;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.model.EvaluacionZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.repository.EvaluacionZonificacionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para el registro de la evaluación técnica de zonificación y compatibilidad de uso (M07 - I06).
 * Reglas de negocio mandatorias:
 * 1. Registra el dictamen emitido por el evaluador municipal competente.
 * 2. NO genera certificados de Edificaciones ni dictámenes de zonificación automáticos.
 * 3. NO infiere compatibilidad únicamente por coordenadas o código CIIU.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluacionZonificacionService {

    private final EvaluacionZonificacionRepository zonificacionRepository;
    private final ExpedienteRepository expedienteRepository;
    private final EstablecimientoRepository establecimientoRepository;
    private final CuentaRepository cuentaRepository;
    private final OperadorCuentaRepository operadorCuentaRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Registra el resultado formal de evaluación de zonificación emitido por el evaluador municipal (M07).
     */
    @Transactional
    public EvaluacionZonificacionDto registrarEvaluacion(
            UUID expedienteId,
            RegistrarEvaluacionZonificacionDto dto,
            UUID evaluadorId,
            String evaluadorNombre
    ) {
        Expediente exp = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));

        if (dto.getEstablecimientoId() != null && exp.getEstablecimientoId() != null) {
            if (!dto.getEstablecimientoId().equals(exp.getEstablecimientoId())) {
                throw new IllegalArgumentException("El establecimiento indicado no coincide con el establecimiento registrado en el expediente.");
            }
        }

        if (dto.getResultado() == ResultadoZonificacion.COMPATIBLE || dto.getResultado() == ResultadoZonificacion.NO_COMPATIBLE) {
            if (dto.getFuenteSustentoTecnico() == null || dto.getFuenteSustentoTecnico().trim().length() < 8) {
                throw new IllegalArgumentException("Los resultados COMPATIBLE o NO_COMPATIBLE requieren especificar la fuente oficial del informe técnico (mínimo 8 caracteres).");
            }
            if (dto.getObservaciones() == null || dto.getObservaciones().trim().length() < 15) {
                throw new IllegalArgumentException("Se requiere motivación detallada en las observaciones (mínimo 15 caracteres) para sustentar la evaluación de zonificación.");
            }
        }

        Establecimiento est = null;
        if (exp.getEstablecimientoId() != null) {
            est = establecimientoRepository.findById(exp.getEstablecimientoId()).orElse(null);
        }

        BigDecimal latitud = est != null ? est.getLatitud() : null;
        BigDecimal longitud = est != null ? est.getLongitud() : null;
        String direccion = (est != null && est.getDireccion() != null) ? est.getDireccion() : exp.getDireccionEstablecimiento();
        String ciiu = (est != null && est.getCiiu() != null) ? est.getCiiu().getCodigo() : exp.getCiiuCodigo();
        String giro = (est != null && est.getGiroPrincipal() != null) ? est.getGiroPrincipal() : exp.getGiroNegocio();
        String zonifPdu = exp.getZonificacion();

        LocalDateTime ahora = LocalDateTime.now();
        UUID evalId = UUID.randomUUID();

        EvaluacionZonificacion eval = EvaluacionZonificacion.builder()
                .id(evalId)
                .expedienteId(expedienteId)
                .establecimientoId(exp.getEstablecimientoId())
                .direccionDeclarada(direccion != null ? direccion : "Sin dirección registrada")
                .latitudDeclarada(latitud)
                .longitudDeclarada(longitud)
                .codigoCiiu(ciiu)
                .giroComercial(giro != null ? giro : "Sin giro registrado")
                .zonificacionPdu(zonifPdu)
                .referenciaZonificacion(dto.getReferenciaZonificacion().trim())
                .fuenteSustentoTecnico(dto.getFuenteSustentoTecnico().trim())
                .resultado(dto.getResultado())
                .evaluadorId(evaluadorId)
                .evaluadorNombre(evaluadorNombre)
                .fechaEvaluacion(ahora)
                .observaciones(dto.getObservaciones().trim())
                .documentoSustentatorioId(dto.getDocumentoSustentatorioId())
                .fechaRegistro(ahora)
                .build();

        eval = zonificacionRepository.save(eval);

        // Actualizar zonificación de referencia en el expediente si no la tenía
        if (exp.getZonificacion() == null || exp.getZonificacion().isBlank()) {
            exp.setZonificacion(zonifPdu != null ? zonifPdu : dto.getReferenciaZonificacion());
            expedienteRepository.save(exp);
        }

        auditoriaService.registrarTransicion(
                expedienteId,
                exp.getEstado(),
                exp.getEstado(),
                "ZONIFICACION_EDIFICACIONES",
                String.format("Evaluación de zonificación registrada por %s: Resultado=%s, Sustento=%s",
                        evaluadorNombre, dto.getResultado(), dto.getFuenteSustentoTecnico())
        );

        log.info("[ZONIFICACION] Evaluación registrada para expediente {} por {}. Resultado: {}, Sustento: {}",
                expedienteId, evaluadorNombre, dto.getResultado(), dto.getFuenteSustentoTecnico());

        return mapearDto(eval, exp.getNumeroTramite());
    }

    /**
     * Consulta la evaluación de zonificación más reciente o la lista de evaluaciones del expediente.
     */
    @Transactional(readOnly = true)
    public List<EvaluacionZonificacionDto> listarEvaluacionesPorExpediente(UUID expedienteId, UUID usuarioId, String username, String roles) {
        Expediente exp = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));

        boolean esFuncionario = roles != null && (roles.contains("ADMIN") || roles.contains("EVALUADOR") || roles.contains("CAJERO"));
        if (!esFuncionario) {
            validarTitularidad(exp, usuarioId, username);
        }

        return zonificacionRepository.findByExpedienteIdOrderByFechaRegistroDesc(expedienteId).stream()
                .map(e -> mapearDto(e, exp.getNumeroTramite()))
                .collect(Collectors.toList());
    }

    /**
     * Consulta la evaluación más reciente de un expediente.
     */
    @Transactional(readOnly = true)
    public Optional<EvaluacionZonificacionDto> obtenerUltimaEvaluacion(UUID expedienteId, UUID usuarioId, String username, String roles) {
        Expediente exp = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));

        boolean esFuncionario = roles != null && (roles.contains("ADMIN") || roles.contains("EVALUADOR") || roles.contains("CAJERO"));
        if (!esFuncionario) {
            validarTitularidad(exp, usuarioId, username);
        }

        return zonificacionRepository.findFirstByExpedienteIdOrderByFechaRegistroDesc(expedienteId)
                .map(e -> mapearDto(e, exp.getNumeroTramite()));
    }

    /**
     * Lista evaluaciones con resultado específico (p. ej. PENDIENTE_EVALUACION o REQUIERE_SUBSANACION).
     */
    @Transactional(readOnly = true)
    public List<EvaluacionZonificacionDto> listarPorResultado(ResultadoZonificacion resultado) {
        return zonificacionRepository.findByResultadoOrderByFechaRegistroAsc(resultado).stream()
                .map(e -> mapearDto(e, null))
                .collect(Collectors.toList());
    }

    private void validarTitularidad(Expediente exp, UUID usuarioId, String username) {
        if (usuarioId == null && username != null) {
            usuarioId = cuentaRepository.findByNumeroDocumento(username)
                    .map(pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta::getId)
                    .orElse(null);
        }

        if (usuarioId != null) {
            if (usuarioId.equals(exp.getSolicitanteId()) || usuarioId.equals(exp.getCuentaId())) {
                return;
            }
        }

        if (exp.getCuentaId() != null && username != null) {
            final UUID finalUsuarioId = usuarioId;
            boolean esOperador = operadorCuentaRepository.findByCuentaId(exp.getCuentaId()).stream()
                    .anyMatch(op -> Boolean.TRUE.equals(op.getActivo()) &&
                            (op.getDniOperador().equalsIgnoreCase(username) ||
                                    (op.getUsuario() != null && op.getUsuario().getUsername().equalsIgnoreCase(username)) ||
                                    (finalUsuarioId != null && op.getUsuario() != null && finalUsuarioId.equals(op.getUsuario().getId()))));
            if (esOperador) return;
        }

        log.warn("[SEGURIDAD-IDOR] Denegado: Usuario {} intentó acceder a zonificación de expediente {}",
                DocumentoSecurityValidator.ofuscarDocumento(username), exp.getId());
        throw new AccessDeniedException("No cuenta con autorización para acceder a la evaluación de zonificación de este expediente.");
    }

    private EvaluacionZonificacionDto mapearDto(EvaluacionZonificacion e, String numeroTramite) {
        return EvaluacionZonificacionDto.builder()
                .id(e.getId())
                .expedienteId(e.getExpedienteId())
                .numeroTramite(numeroTramite)
                .establecimientoId(e.getEstablecimientoId())
                .direccionDeclarada(e.getDireccionDeclarada())
                .latitudDeclarada(e.getLatitudDeclarada())
                .longitudDeclarada(e.getLongitudDeclarada())
                .codigoCiiu(e.getCodigoCiiu())
                .giroComercial(e.getGiroComercial())
                .zonificacionPdu(e.getZonificacionPdu())
                .referenciaZonificacion(e.getReferenciaZonificacion())
                .fuenteSustentoTecnico(e.getFuenteSustentoTecnico())
                .resultado(e.getResultado())
                .evaluadorId(e.getEvaluadorId())
                .evaluadorNombre(e.getEvaluadorNombre())
                .fechaEvaluacion(e.getFechaEvaluacion())
                .observaciones(e.getObservaciones())
                .documentoSustentatorioId(e.getDocumentoSustentatorioId())
                .fechaRegistro(e.getFechaRegistro())
                .build();
    }
}
