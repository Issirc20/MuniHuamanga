package pe.gob.munihuamanga.licencias.expedientes.pagos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.DeclararPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialDecisionPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.PagoExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.RechazarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarPagoManualDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.borradores.security.DocumentoSecurityValidator;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ComandosExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.HistorialDecisionPago;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.PagoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.HistorialDecisionPagoRepository;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.PagoExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.RolesInstitucionales;
import pe.gob.munihuamanga.licencias.expedientes.security.VerificadorTitularidad;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para la recepción, revisión y validación manual de pagos SAT (M05 - I06).
 * Reglas mandatorias:
 * 1. La carga de voucher registra el estado PENDIENTE_VALIDACION (nunca pago confirmado ni automático).
 * 2. Solo CAJERO o ADMIN pueden realizar la validación manual.
 * 3. Se registra justificación y evidencia en el historial de decisiones.
 * 4. Un comprobante no puede asociarse a múltiples expedientes.
 * 5. Importes contrastados contra la tasa TUPA oficial (Ordenanza N° 018-2024-MPH).
 * 6. La validación manual NO transiciona automáticamente el expediente a APROBADO ni a DOCUMENTOS_VALIDADOS (I06.1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoExpedienteService {

    private final PagoExpedienteRepository pagoRepository;
    private final HistorialDecisionPagoRepository historialRepository;
    private final ExpedienteRepository expedienteRepository;
    private final AuditoriaService auditoriaService;
    private final ComandosExpediente comandosExpediente;
    private final VerificadorTitularidad verificadorTitularidad;

    public static String normalizarCodigoComprobante(String codigo) {
        if (codigo == null) return null;
        return codigo.trim()
                .toUpperCase()
                .replaceAll("\\s*-\\s*", "-")
                .replaceAll("\\s*/\\s*", "/")
                .replaceAll("\\s+", " ");
    }

    private void validarFormatoCodigoComprobante(String codigo) {
        if (codigo == null || codigo.length() < 3 || codigo.length() > 50) {
            throw new IllegalArgumentException("El código de comprobante debe tener entre 3 y 50 caracteres.");
        }
        if (!codigo.matches("^[A-Z0-9][A-Z0-9_/-]{1,48}[A-Z0-9]$")) {
            throw new IllegalArgumentException("El código de comprobante contiene caracteres no permitidos. Use letras, números, guiones o barras.");
        }
    }

    private PagoExpediente obtenerPagoConBloqueo(UUID pagoId) {
        return pagoRepository.findByIdWithLock(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Registro de pago no encontrado con ID: " + pagoId));
    }

    /**
     * Declara un comprobante de pago para un expediente formal.
     */
    @Transactional
    public PagoExpedienteDto declararPago(UUID expedienteId, DeclararPagoDto dto) {
        Expediente exp = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));

        Actor actor = verificadorTitularidad.exigir(exp, RolesInstitucionales.ATENCION_PRESENCIAL, PermisoOperador.PRESENTAR_EXPEDIENTE);
        UUID usuarioId = actor.usuarioId();
        String username = actor.nombreParaRegistro();

        if (exp.getEstado() != EstadoExpediente.PENDIENTE_PAGO) {
            throw new IllegalStateException("El comprobante de pago se registra una vez emitida la liquidación de la tasa. Estado actual del expediente: "
                    + exp.getEstado() + ".");
        }

        String codigoLimpio = normalizarCodigoComprobante(dto.getCodigoComprobante());
        validarFormatoCodigoComprobante(codigoLimpio);

        if (dto.getFechaDeclarada() != null && dto.getFechaDeclarada().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha declarada del comprobante no puede ser futura.");
        }

        if (dto.getMontoDeclarado() == null || dto.getMontoDeclarado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto declarado debe ser mayor a cero.");
        }

        if (pagoRepository.existsByCodigoComprobante(codigoLimpio)) {
            log.warn("[PAGOS-SAT] Intento de registrar comprobante duplicado: {}", codigoLimpio);
            throw new IllegalArgumentException("El código de comprobante " + codigoLimpio + " ya se encuentra registrado en el sistema.");
        }

        BigDecimal tasaEsperada = exp.getMontoTasa();
        if (tasaEsperada == null) {
            throw new IllegalStateException("El expediente no cuenta con liquidación definitiva de la tasa.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        UUID pagoId = UUID.randomUUID();

        PagoExpediente pago = PagoExpediente.builder()
                .id(pagoId)
                .expedienteId(expedienteId)
                .codigoComprobante(codigoLimpio)
                .fechaDeclarada(dto.getFechaDeclarada())
                .montoDeclarado(dto.getMontoDeclarado())
                .documentoId(dto.getDocumentoSustentatorioId())
                .observacionesAdministrado(dto.getObservacionesAdministrado())
                .estado(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .tasaTupaEsperada(tasaEsperada)
                .fechaRegistro(ahora)
                .fechaActualizacion(ahora)
                .build();

        pago = pagoRepository.save(pago);

        // Registrar auditoría de decisión inicial
        HistorialDecisionPago decision = HistorialDecisionPago.builder()
                .id(UUID.randomUUID())
                .pagoId(pagoId)
                .estadoAnterior(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .estadoNuevo(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .usuarioId(usuarioId)
                .usuarioNombre(DocumentoSecurityValidator.ofuscarDocumento(username))
                .motivo("Comprobante declarado por el administrado; pendiente de cotejo y validación manual en ventanilla/caja")
                .fechaDecision(ahora)
                .build();
        historialRepository.save(decision);

        log.info("[PAGOS-SAT] Comprobante {} declarado para expediente {} por usuario {}. Estado: PENDIENTE_VALIDACION",
                codigoLimpio, expedienteId, DocumentoSecurityValidator.ofuscarDocumento(username));

        return mapearDto(pago, exp.getNumeroTramite());
    }

    /**
     * Pone un comprobante en revisión por el cajero municipal.
     */
    @Transactional
    public PagoExpedienteDto iniciarRevision(UUID pagoId, UUID cajeroId, String cajeroNombre) {
        PagoExpediente pago = obtenerPagoConBloqueo(pagoId);
        EstadoValidacionPago anterior = pago.getEstado();

        if (anterior == EstadoValidacionPago.VALIDADO_MANUALMENTE) {
            throw new IllegalStateException("El comprobante ya fue validado manualmente con anterioridad.");
        }
        if (anterior == EstadoValidacionPago.RECHAZADO) {
            throw new IllegalStateException("El comprobante fue rechazado y no puede ser reabierto.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        pago.setEstado(EstadoValidacionPago.EN_REVISION);
        pago.setValidadorId(cajeroId);
        pago.setValidadorNombre(cajeroNombre);
        pago.setFechaActualizacion(ahora);
        pagoRepository.save(pago);

        HistorialDecisionPago decision = HistorialDecisionPago.builder()
                .id(UUID.randomUUID())
                .pagoId(pagoId)
                .estadoAnterior(anterior)
                .estadoNuevo(EstadoValidacionPago.EN_REVISION)
                .usuarioId(cajeroId)
                .usuarioNombre(cajeroNombre)
                .motivo("Comprobante tomado en revisión y cotejo manual por el cajero")
                .fechaDecision(ahora)
                .build();
        historialRepository.save(decision);

        return mapearDto(pago, null);
    }

    /**
     * Valida manualmente el comprobante de pago cotejado por el cajero (M05).
     * En conformidad con I06.1:
     * - Bloqueo pesimista concurrente (findByIdWithLock).
     * - Impide doble validación (IllegalStateException).
     * - Impide validar comprobantes RECHAZADOS (IllegalStateException).
     * - Exige justificación de cotejo (mínimo 10 caracteres).
     * - Verifica importe frente a la tasa TUPA oficial institucional.
     * - INDEPENDENCIA DE ESTADOS: NO muta automáticamente el expediente a DOCUMENTOS_VALIDADOS ni APROBADO.
     */
    @Transactional
    public PagoExpedienteDto validarPagoManual(UUID pagoId, ValidarPagoManualDto dto, UUID cajeroId, String cajeroNombre) {
        PagoExpediente pago = obtenerPagoConBloqueo(pagoId);
        EstadoValidacionPago anterior = pago.getEstado();

        if (anterior == EstadoValidacionPago.VALIDADO_MANUALMENTE) {
            throw new IllegalStateException("El comprobante ya fue validado manualmente con anterioridad.");
        }
        if (anterior == EstadoValidacionPago.RECHAZADO) {
            throw new IllegalStateException("No se puede validar un comprobante que ha sido RECHAZADO.");
        }

        if (dto.getJustificacion() == null || dto.getJustificacion().trim().length() < 10) {
            throw new IllegalArgumentException("La justificación de cotejo manual debe contener al menos 10 caracteres.");
        }

        if (pago.getTasaTupaEsperada() != null && dto.getMontoValidado().compareTo(pago.getTasaTupaEsperada()) < 0) {
            throw new IllegalArgumentException(String.format(
                    "El monto validado (S/. %s) es inferior a la tasa TUPA oficial exigida (S/. %s) según Ordenanza N° 018-2024-MPH.",
                    dto.getMontoValidado(), pago.getTasaTupaEsperada()));
        }

        LocalDateTime ahora = LocalDateTime.now();
        pago.setEstado(EstadoValidacionPago.VALIDADO_MANUALMENTE);
        pago.setNumeroOperacionSat(dto.getNumeroOperacionSat().trim());
        pago.setMontoValidado(dto.getMontoValidado());
        pago.setFechaPagoReal(dto.getFechaPagoReal());
        pago.setValidadorId(cajeroId);
        pago.setValidadorNombre(cajeroNombre);
        pago.setFechaValidacion(ahora);
        pago.setFechaActualizacion(ahora);
        pagoRepository.save(pago);

        HistorialDecisionPago decision = HistorialDecisionPago.builder()
                .id(UUID.randomUUID())
                .pagoId(pagoId)
                .estadoAnterior(anterior)
                .estadoNuevo(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .usuarioId(cajeroId)
                .usuarioNombre(cajeroNombre)
                .motivo(dto.getJustificacion().trim())
                .fechaDecision(ahora)
                .build();
        historialRepository.save(decision);

        // Con el pago validado el expediente queda completo: se inicia la evaluación y corre el plazo legal.
        Expediente exp = expedienteRepository.findById(pago.getExpedienteId()).orElse(null);
        if (exp != null) {
            exp.setVoucherId(pago.getCodigoComprobante());
            exp.setNumeroOperacionSat(dto.getNumeroOperacionSat().trim());
            exp.setFechaPagoSat(dto.getFechaPagoReal().atStartOfDay());
            expedienteRepository.save(exp);

            String sustento = String.format("Pago de la tasa validado. Comprobante: %s, operación SAT: %s, monto: S/ %s. %s",
                    pago.getCodigoComprobante(), dto.getNumeroOperacionSat().trim(), dto.getMontoValidado(), dto.getJustificacion().trim());
            if (exp.getEstado() == EstadoExpediente.PENDIENTE_PAGO) {
                comandosExpediente.registrar(exp.getId(), EventoExpediente.PAGO_VALIDADO, sustento);
            } else {
                auditoriaService.registrarTransicion(exp.getId(), exp.getEstado(), exp.getEstado(), cajeroNombre, sustento);
            }
        }

        log.info("[PAGOS-SAT] Pago {} validado manualmente por {} para expediente {}. Monto: S/. {}",
                pago.getCodigoComprobante(), cajeroNombre, pago.getExpedienteId(), dto.getMontoValidado());

        return mapearDto(pago, exp != null ? exp.getNumeroTramite() : null);
    }

    /**
     * Observa un comprobante de pago por inconsistencias.
     */
    @Transactional
    public PagoExpedienteDto observarPago(UUID pagoId, ObservarPagoDto dto, UUID cajeroId, String cajeroNombre) {
        PagoExpediente pago = obtenerPagoConBloqueo(pagoId);
        EstadoValidacionPago anterior = pago.getEstado();

        if (anterior == EstadoValidacionPago.VALIDADO_MANUALMENTE) {
            throw new IllegalStateException("No se puede observar un comprobante que ya se encuentra VALIDADO_MANUALMENTE.");
        }
        if (anterior == EstadoValidacionPago.RECHAZADO) {
            throw new IllegalStateException("No se puede observar un comprobante que ha sido RECHAZADO.");
        }
        if (dto.getMotivo() == null || dto.getMotivo().trim().length() < 5) {
            throw new IllegalArgumentException("El motivo de observación debe contener al menos 5 caracteres.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        pago.setEstado(EstadoValidacionPago.OBSERVADO);
        pago.setMotivoObservacionRechazo(dto.getMotivo().trim());
        pago.setValidadorId(cajeroId);
        pago.setValidadorNombre(cajeroNombre);
        pago.setFechaActualizacion(ahora);
        pagoRepository.save(pago);

        HistorialDecisionPago decision = HistorialDecisionPago.builder()
                .id(UUID.randomUUID())
                .pagoId(pagoId)
                .estadoAnterior(anterior)
                .estadoNuevo(EstadoValidacionPago.OBSERVADO)
                .usuarioId(cajeroId)
                .usuarioNombre(cajeroNombre)
                .motivo(dto.getMotivo().trim())
                .fechaDecision(ahora)
                .build();
        historialRepository.save(decision);

        log.info("[PAGOS-SAT] Pago {} observado por {}. Motivo: {}", pago.getCodigoComprobante(), cajeroNombre, dto.getMotivo());
        return mapearDto(pago, null);
    }

    /**
     * Rechaza un comprobante de pago por falsedad, prescripción o no corresponder al trámite.
     */
    @Transactional
    public PagoExpedienteDto rechazarPago(UUID pagoId, RechazarPagoDto dto, UUID cajeroId, String cajeroNombre) {
        PagoExpediente pago = obtenerPagoConBloqueo(pagoId);
        EstadoValidacionPago anterior = pago.getEstado();

        if (anterior == EstadoValidacionPago.VALIDADO_MANUALMENTE) {
            throw new IllegalStateException("No se puede rechazar un comprobante que ya se encuentra VALIDADO_MANUALMENTE.");
        }
        if (anterior == EstadoValidacionPago.RECHAZADO) {
            throw new IllegalStateException("El comprobante ya fue rechazado anteriormente.");
        }
        if (dto.getMotivo() == null || dto.getMotivo().trim().length() < 5) {
            throw new IllegalArgumentException("El motivo de rechazo debe contener al menos 5 caracteres.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        pago.setEstado(EstadoValidacionPago.RECHAZADO);
        pago.setMotivoObservacionRechazo(dto.getMotivo().trim());
        pago.setValidadorId(cajeroId);
        pago.setValidadorNombre(cajeroNombre);
        pago.setFechaActualizacion(ahora);
        pagoRepository.save(pago);

        HistorialDecisionPago decision = HistorialDecisionPago.builder()
                .id(UUID.randomUUID())
                .pagoId(pagoId)
                .estadoAnterior(anterior)
                .estadoNuevo(EstadoValidacionPago.RECHAZADO)
                .usuarioId(cajeroId)
                .usuarioNombre(cajeroNombre)
                .motivo(dto.getMotivo().trim())
                .fechaDecision(ahora)
                .build();
        historialRepository.save(decision);

        log.info("[PAGOS-SAT] Pago {} rechazado por {}. Motivo: {}", pago.getCodigoComprobante(), cajeroNombre, dto.getMotivo());
        return mapearDto(pago, null);
    }

    /**
     * Lista los pagos registrados para un expediente específico.
     */
    @Transactional(readOnly = true)
    public List<PagoExpedienteDto> listarPagosPorExpediente(UUID expedienteId) {
        Expediente exp = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));
        verificadorTitularidad.exigir(exp, RolesInstitucionales.CONSULTA_EXPEDIENTES, PermisoOperador.CONSULTAR_EXPEDIENTE);

        return pagoRepository.findByExpedienteIdOrderByFechaRegistroDesc(expedienteId).stream()
                .map(p -> mapearDto(p, exp.getNumeroTramite()))
                .collect(Collectors.toList());
    }

    /**
     * Consulta con filtros para la bandeja administrativa de caja.
     */
    @Transactional(readOnly = true)
    public List<PagoExpedienteDto> buscarPagosConFiltros(EstadoValidacionPago estado, UUID expedienteId, LocalDate fechaDesde, LocalDate fechaHasta) {
        return pagoRepository.buscarConFiltros(estado, expedienteId, fechaDesde, fechaHasta).stream()
                .map(p -> mapearDto(p, null))
                .collect(Collectors.toList());
    }

    /**
     * Consulta un pago individual con auditoría.
     */
    @Transactional(readOnly = true)
    public PagoExpedienteDto obtenerPagoPorId(UUID pagoId) {
        PagoExpediente pago = obtenerPago(pagoId);
        Expediente exp = exigirAccesoAlExpedienteDelPago(pago);
        return mapearDto(pago, exp.getNumeroTramite());
    }

    /**
     * Obtiene el historial de decisiones de un pago.
     */
    @Transactional(readOnly = true)
    public List<HistorialDecisionPagoDto> obtenerHistorialPago(UUID pagoId) {
        PagoExpediente pago = obtenerPago(pagoId);
        exigirAccesoAlExpedienteDelPago(pago);

        return historialRepository.findByPagoIdOrderByFechaDecisionDesc(pagoId).stream()
                .map(this::mapearHistorialDto)
                .collect(Collectors.toList());
    }

    private PagoExpediente obtenerPago(UUID pagoId) {
        return pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Registro de pago no encontrado con ID: " + pagoId));
    }

    private Expediente exigirAccesoAlExpedienteDelPago(PagoExpediente pago) {
        Expediente exp = expedienteRepository.findById(pago.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", pago.getExpedienteId()));
        verificadorTitularidad.exigir(exp, RolesInstitucionales.CONSULTA_EXPEDIENTES, PermisoOperador.CONSULTAR_EXPEDIENTE);
        return exp;
    }

    private PagoExpedienteDto mapearDto(PagoExpediente pago, String numeroTramite) {
        return PagoExpedienteDto.builder()
                .id(pago.getId())
                .expedienteId(pago.getExpedienteId())
                .numeroTramite(numeroTramite)
                .codigoComprobante(pago.getCodigoComprobante())
                .fechaDeclarada(pago.getFechaDeclarada())
                .montoDeclarado(pago.getMontoDeclarado())
                .documentoId(pago.getDocumentoId())
                .observacionesAdministrado(pago.getObservacionesAdministrado())
                .estado(pago.getEstado())
                .validadorId(pago.getValidadorId())
                .validadorNombre(pago.getValidadorNombre())
                .fechaValidacion(pago.getFechaValidacion())
                .motivoObservacionORechazo(pago.getMotivoObservacionRechazo())
                .numeroOperacionSat(pago.getNumeroOperacionSat())
                .fechaPagoReal(pago.getFechaPagoReal())
                .montoValidado(pago.getMontoValidado())
                .tasaTupaEsperada(pago.getTasaTupaEsperada())
                .fechaRegistro(pago.getFechaRegistro())
                .fechaActualizacion(pago.getFechaActualizacion())
                .build();
    }

    private HistorialDecisionPagoDto mapearHistorialDto(HistorialDecisionPago h) {
        return HistorialDecisionPagoDto.builder()
                .id(h.getId())
                .pagoId(h.getPagoId())
                .estadoAnterior(h.getEstadoAnterior())
                .estadoNuevo(h.getEstadoNuevo())
                .usuarioId(h.getUsuarioId())
                .usuarioNombre(h.getUsuarioNombre())
                .motivo(h.getMotivo())
                .fechaDecision(h.getFechaDecision())
                .build();
    }
}
