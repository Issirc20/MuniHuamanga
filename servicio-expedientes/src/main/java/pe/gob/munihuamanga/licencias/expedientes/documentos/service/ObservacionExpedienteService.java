package pe.gob.munihuamanga.licencias.expedientes.documentos.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.CrearObservacionDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservacionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.SubsanarObservacionesDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoObservacion;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.borradores.security.DocumentoSecurityValidator;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.ObservacionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.ObservacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;
import pe.gob.munihuamanga.licencias.expedientes.validator.EstadoExpedienteValidator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de observaciones técnicas/documentales y subsanaciones (Incremento I05 / I05.1).
 * Aplica con rigor la Ley N° 27444 LPAG (Art. 136 y 143), el D.S. N° 002-2018-PCM y el TUPA MPH.
 */
@Slf4j
@Service
public class ObservacionExpedienteService {

    private final ObservacionExpedienteRepository observacionRepository;
    private final DocumentoVersionadoRepository documentoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final OperadorCuentaRepository operadorCuentaRepository;
    private final CuentaRepository cuentaRepository;
    private final EstadoExpedienteValidator estadoValidator;
    private final AuditoriaService auditoriaService;
    private final NotificacionEmailService notificacionEmailService;

    public ObservacionExpedienteService(ObservacionExpedienteRepository observacionRepository,
                                       DocumentoVersionadoRepository documentoRepository,
                                       ExpedienteRepository expedienteRepository,
                                       OperadorCuentaRepository operadorCuentaRepository,
                                       CuentaRepository cuentaRepository,
                                       EstadoExpedienteValidator estadoValidator,
                                       AuditoriaService auditoriaService,
                                       NotificacionEmailService notificacionEmailService) {
        this.observacionRepository = observacionRepository;
        this.documentoRepository = documentoRepository;
        this.expedienteRepository = expedienteRepository;
        this.operadorCuentaRepository = operadorCuentaRepository;
        this.cuentaRepository = cuentaRepository;
        this.estadoValidator = estadoValidator;
        this.auditoriaService = auditoriaService;
        this.notificacionEmailService = notificacionEmailService;
    }

    /**
     * Formula una o más observaciones sobre un expediente por funcionarios autorizados (EVALUADOR/ADMIN).
     * Transiciona el expediente a estado OBSERVADO con cálculo exacto de días hábiles.
     */
    @Transactional
    public List<ObservacionExpedienteDto> formularObservaciones(UUID expedienteId,
                                                               List<CrearObservacionDto> dtos,
                                                               UUID evaluadorId,
                                                               String evaluadorUsername) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));

        EstadoExpediente actual = expediente.getEstado();
        if (actual == EstadoExpediente.APROBADO || actual == EstadoExpediente.RECHAZADO) {
            throw new IllegalStateException("No se pueden formular observaciones a un expediente concluido o resuelto. Estado actual: " + actual);
        }

        estadoValidator.validarTransicion(actual, EstadoExpediente.OBSERVADO);

        LocalDateTime ahora = LocalDateTime.now();
        List<ObservacionExpediente> guardadas = new ArrayList<>();
        StringBuilder motivosAcumulados = new StringBuilder();

        for (CrearObservacionDto dto : dtos) {
            TipoObservacion tipo = dto.getTipoObservacion() != null
                    ? dto.getTipoObservacion()
                    : TipoObservacion.EVALUACION_TECNICA_ITSE;

            int plazo = (dto.getPlazoDiasHabiles() != null && dto.getPlazoDiasHabiles() > 0)
                    ? dto.getPlazoDiasHabiles()
                    : tipo.getPlazoDiasHabilesDefault();

            String fundamento = (dto.getFundamentoLegal() != null && !dto.getFundamentoLegal().isBlank())
                    ? dto.getFundamentoLegal()
                    : tipo.getBaseLegalDefault();

            LocalDateTime fechaLimite = calcularFechaLimiteDiasHabiles(ahora, plazo);

            ObservacionExpediente obs = ObservacionExpediente.builder()
                    .id(UUID.randomUUID())
                    .expedienteId(expedienteId)
                    .documentoId(dto.getDocumentoId())
                    .tipoObservacion(tipo)
                    .requisitoAfectado(dto.getRequisitoAfectado())
                    .motivo(dto.getMotivo())
                    .fundamentoLegal(fundamento)
                    .fechaObservacion(ahora)
                    .observadorId(evaluadorId)
                    .observadorUsername(evaluadorUsername)
                    .plazoDiasHabiles(plazo)
                    .fechaLimiteSubsanacion(fechaLimite)
                    .subsanada(false)
                    .build();

            guardadas.add(observacionRepository.save(obs));

            if (dto.getDocumentoId() != null) {
                documentoRepository.findById(dto.getDocumentoId()).ifPresent(doc -> {
                    doc.setEstadoRevision(EstadoRevisionDocumento.OBSERVADO);
                    documentoRepository.save(doc);
                });
            }

            if (!motivosAcumulados.isEmpty()) motivosAcumulados.append(" | ");
            motivosAcumulados.append("[").append(dto.getRequisitoAfectado()).append("] ").append(dto.getMotivo());
        }

        // Transicionar expediente a OBSERVADO
        expediente.setMotivoObservacion(motivosAcumulados.toString());
        expediente.setFechaObservacion(ahora);
        expediente.cambiarEstado(EstadoExpediente.OBSERVADO);
        expedienteRepository.save(expediente);

        // Registro de auditoría institucional
        auditoriaService.registrarTransicion(
                expedienteId,
                actual,
                EstadoExpediente.OBSERVADO,
                evaluadorUsername,
                "Observaciones formuladas: " + motivosAcumulados
        );

        // Notificación por correo electrónico
        notificacionEmailService.notificarObservacion(expediente, motivosAcumulados.toString());

        log.info("[OBSERVACION-EXPEDIENTE] {} observaciones registradas para expediente {} por evaluador {}",
                guardadas.size(), expedienteId, evaluadorUsername);

        return guardadas.stream().map(this::mapearDto).collect(Collectors.toList());
    }

    /**
     * Consulta el catálogo de observaciones formuladas al expediente.
     */
    @Transactional(readOnly = true)
    public List<ObservacionExpedienteDto> listarObservaciones(UUID expedienteId,
                                                             UUID usuarioId,
                                                             String username,
                                                             String rolUsuario) {
        Expediente exp = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));

        boolean esFuncionario = rolUsuario != null && (rolUsuario.contains("ADMIN") || rolUsuario.contains("EVALUADOR"));
        validarTitularidadExpediente(exp, usuarioId, username, esFuncionario);

        return observacionRepository.findByExpedienteIdOrderByFechaObservacionDesc(expedienteId).stream()
                .map(this::mapearDto)
                .collect(Collectors.toList());
    }

    /**
     * Subsanación formal presentada por el administrado (Art. 136 Ley N° 27444).
     * Mantiene estrictamente el mismo número de expediente y transiciona al flujo correspondiente.
     * Exige que todas las observaciones pendientes sean satisfechas antes de transicionar a SUBSANADO.
     */
    @Transactional
    public void subsanarObservaciones(UUID expedienteId,
                                      SubsanarObservacionesDto dto,
                                      UUID usuarioId,
                                      String username,
                                      String rolUsuario) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));

        EstadoExpediente actual = expediente.getEstado();
        if (actual != EstadoExpediente.OBSERVADO) {
            throw new IllegalStateException("Solo se pueden subsanar expedientes en estado OBSERVADO. Estado actual: " + actual);
        }

        boolean esFuncionario = rolUsuario != null && (rolUsuario.contains("ADMIN") || rolUsuario.contains("EVALUADOR"));
        validarTitularidadExpediente(expediente, usuarioId, username, esFuncionario);

        List<ObservacionExpediente> observacionesAProcesar;
        if (dto.getObservacionesIds() != null && !dto.getObservacionesIds().isEmpty()) {
            observacionesAProcesar = new ArrayList<>();
            for (UUID obsId : dto.getObservacionesIds()) {
                ObservacionExpediente obs = observacionRepository.findById(obsId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Observación no encontrada: " + obsId));
                if (!obs.getExpedienteId().equals(expedienteId)) {
                    throw new IllegalArgumentException("La observación " + obsId + " no pertenece al expediente " + expedienteId);
                }
                observacionesAProcesar.add(obs);
            }
        } else {
            observacionesAProcesar = observacionRepository.findByExpedienteIdAndSubsanadaFalse(expedienteId);
        }

        LocalDateTime ahora = LocalDateTime.now();

        // Verificar que las observaciones documentales tengan nueva versión cargada
        for (ObservacionExpediente obs : observacionesAProcesar) {
            if (obs.getDocumentoId() != null && obs.getDocumentoSubsanacionId() == null) {
                DocumentoVersionado docOriginal = documentoRepository.findById(obs.getDocumentoId()).orElse(null);
                if (docOriginal != null) {
                    Optional<DocumentoVersionado> versionReciente = documentoRepository
                            .findFirstByExpedienteIdAndTipoDocumentoOrderByVersionDesc(expedienteId, docOriginal.getTipoDocumento());
                    if (versionReciente.isEmpty() || versionReciente.get().getVersion() <= docOriginal.getVersion()) {
                        throw new IllegalStateException("No se puede subsanar la observación sobre " + obs.getRequisitoAfectado() +
                                " porque aún no se ha adjuntado una nueva versión corregida del documento.");
                    }
                    obs.setDocumentoSubsanacionId(versionReciente.get().getId());
                }
            }
            obs.setSubsanada(true);
            obs.setFechaSubsanacion(ahora);
            obs.setDetalleSubsanacion(dto.getDetalleSubsanacion());
            observacionRepository.save(obs);
        }

        // REGLA DE INTEGRIDAD I05.1: Comprobar que no queden observaciones pendientes
        boolean quedanPendientes = observacionRepository.existsByExpedienteIdAndSubsanadaFalse(expedienteId);
        if (quedanPendientes) {
            log.warn("[SUBSANACION-PARCIAL] Expediente {} aún tiene observaciones pendientes no subsanadas.", expedienteId);
            throw new IllegalStateException("Quedan observaciones pendientes por subsanar. No se puede transicionar el expediente a SUBSANADO hasta satisfacer la totalidad de observaciones.");
        }

        // Determinar estado objetivo oficial V2
        EstadoExpediente nuevo = EstadoExpediente.SUBSANADO;

        estadoValidator.validarTransicion(actual, nuevo);

        expediente.setDetalleSubsanacion(dto.getDetalleSubsanacion());
        expediente.setFechaSubsanacion(ahora);
        expediente.cambiarEstado(nuevo);
        expedienteRepository.save(expediente);

        String responsable = (username != null && !username.isBlank()) ? username : expediente.getNombreTitular();
        auditoriaService.registrarTransicion(
                expedienteId,
                actual,
                nuevo,
                responsable,
                "Subsanación integral de observaciones (LPAG Art. 136): " + dto.getDetalleSubsanacion()
        );

        log.info("[SUBSANACION-EXPEDIENTE] Expediente {} subsanado exitosamente. Nuevo estado: {}", expedienteId, nuevo);
    }

    /**
     * Calcula la fecha límite de subsanación sumando únicamente días hábiles (Lunes a Viernes),
     * excluyendo fines de semana y feriados oficiales nacionales conforme a la LPAG (Art. 145).
     */
    public LocalDateTime calcularFechaLimiteDiasHabiles(LocalDateTime fechaInicio, int diasHabiles) {
        LocalDateTime cursor = fechaInicio;
        int agregados = 0;
        while (agregados < diasHabiles) {
            cursor = cursor.plusDays(1);
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !esFeriadoNacional(cursor.toLocalDate())) {
                agregados++;
            }
        }
        return cursor.withHour(23).withMinute(59).withSecond(59);
    }

    private boolean esFeriadoNacional(LocalDate fecha) {
        int mes = fecha.getMonthValue();
        int dia = fecha.getDayOfMonth();
        // Feriados oficiales en Perú (D. Leg. 713 y normas conexas)
        if (mes == 1 && dia == 1) return true;   // Año Nuevo
        if (mes == 5 && dia == 1) return true;   // Día del Trabajo
        if (mes == 6 && dia == 29) return true;  // San Pedro y San Pablo
        if (mes == 7 && (dia == 28 || dia == 29)) return true; // Fiestas Patrias
        if (mes == 8 && (dia == 6 || dia == 30)) return true;  // Batalla de Junín / Santa Rosa
        if (mes == 10 && dia == 8) return true;  // Combate de Angamos
        if (mes == 11 && dia == 1) return true;  // Todos los Santos
        if (mes == 12 && (dia == 8 || dia == 9 || dia == 25)) return true; // Inmaculada Concepción / Batalla de Ayacucho / Navidad
        return false;
    }

    private void validarTitularidadExpediente(Expediente exp, UUID usuarioId, String username, boolean esFuncionario) {
        if (esFuncionario) return;

        UUID cuentaOUsuarioId = usuarioId;
        if (cuentaOUsuarioId == null && username != null) {
            cuentaOUsuarioId = cuentaRepository.findByNumeroDocumento(username)
                    .map(pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta::getId)
                    .orElse(null);
        }

        if (cuentaOUsuarioId != null) {
            if (cuentaOUsuarioId.equals(exp.getSolicitanteId()) || cuentaOUsuarioId.equals(exp.getCuentaId())) {
                return;
            }
        }

        if (exp.getCuentaId() != null && username != null) {
            final UUID finalCuentaOUsuarioId = cuentaOUsuarioId;
            boolean esOperador = operadorCuentaRepository.findByCuentaId(exp.getCuentaId()).stream()
                    .anyMatch(op -> Boolean.TRUE.equals(op.getActivo()) &&
                            (op.getDniOperador().equalsIgnoreCase(username) ||
                                    (op.getUsuario() != null && op.getUsuario().getUsername().equalsIgnoreCase(username)) ||
                                    (finalCuentaOUsuarioId != null && op.getUsuario() != null && finalCuentaOUsuarioId.equals(op.getUsuario().getId()))));
            if (esOperador) return;
        }

        // REGLA I05.1: Se elimina tajantemente la coincidencia simple username == documentoIdentidad.
        // Los expedientes históricos V1 no reclamados quedan bajo acceso municipal exclusivo.
        log.warn("[SEGURIDAD - IDOR] Denegado: Usuario {} intentó acceder a observaciones de expediente {} sin titularidad persistente verificada",
                DocumentoSecurityValidator.ofuscarDocumento(username), exp.getId());
        throw new AccessDeniedException("No cuenta con autorización para acceder a las observaciones de este expediente.");
    }

    private ObservacionExpedienteDto mapearDto(ObservacionExpediente obs) {
        return ObservacionExpedienteDto.builder()
                .id(obs.getId())
                .expedienteId(obs.getExpedienteId())
                .documentoId(obs.getDocumentoId())
                .tipoObservacion(obs.getTipoObservacion())
                .requisitoAfectado(obs.getRequisitoAfectado())
                .motivo(obs.getMotivo())
                .fundamentoLegal(obs.getFundamentoLegal())
                .fechaObservacion(obs.getFechaObservacion())
                .observadorUsername(obs.getObservadorUsername())
                .plazoDiasHabiles(obs.getPlazoDiasHabiles())
                .fechaLimiteSubsanacion(obs.getFechaLimiteSubsanacion())
                .subsanada(obs.getSubsanada())
                .fechaSubsanacion(obs.getFechaSubsanacion())
                .documentoSubsanacionId(obs.getDocumentoSubsanacionId())
                .detalleSubsanacion(obs.getDetalleSubsanacion())
                .build();
    }
}
