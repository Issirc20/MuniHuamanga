package pe.gob.munihuamanga.licencias.expedientes.resoluciones.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.AprobarResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.ChecklistAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.EstadoFirmaDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolucionAdministrativaDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitarFirmaDto;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.ChecklistAdministrativoService;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.FirmaDigitalPort;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.ResultadoFirmaDigital;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.HistorialResolucion;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.HistorialResolucionRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.ResolucionAdministrativaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de Gestión de Resoluciones Administrativas (M09 - Incremento I08).
 * Administra el ciclo de vida, segregación estricta de funciones y conexión con firma digital institucional.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResolucionAdministrativaService {

    private final ResolucionAdministrativaRepository resolucionRepository;
    private final HistorialResolucionRepository historialResolucionRepository;
    private final ExpedienteRepository expedienteRepository;
    private final InformeAdministrativoRepository informeAdministrativoRepository;
    private final ChecklistAdministrativoService checklistAdministrativoService;
    private final SecuenciaResolucionService secuenciaResolucionService;
    private final ResolucionPdfGenerator resolucionPdfGenerator;
    private final FirmaDigitalPort firmaDigitalPort;
    private final UsuarioRepository usuarioRepository;
    private final pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.OperacionFirmaRepository operacionFirmaRepository;

    @Transactional
    public ResolucionAdministrativaDto crearBorrador(CrearResolucionDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        log.info("[RESOLUCION] Creando borrador de resolución para expediente {} por {}", dto.getExpedienteId(), actor);

        Expediente exp = expedienteRepository.findById(dto.getExpedienteId())
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + dto.getExpedienteId()));

        // Debe contar con un informe administrativo aprobado vigente
        InformeAdministrativo inf = informeAdministrativoRepository.findByExpedienteIdAndEsVersionActualTrue(exp.getId())
                .orElseThrow(() -> new IllegalStateException("El expediente no cuenta con un informe administrativo técnico registrado"));

        if (inf.getEstado() != EstadoInforme.APROBADO) {
            throw new IllegalStateException("No se puede emitir resolución sin informe administrativo previamente APROBADO por la autoridad técnica");
        }

        // Si la decisión pretendida es FAVORABLE, el expediente debe estar en APTO_PARA_RESOLUCION
        if (dto.getTipoDecision() == TipoDecisionResolucion.FAVORABLE) {
            if (exp.getEstado() != EstadoExpediente.APTO_PARA_RESOLUCION) {
                throw new IllegalStateException("Para emitir resolución FAVORABLE el expediente debe encontrarse en estado APTO_PARA_RESOLUCION. Estado actual: " + exp.getEstado());
            }
            if (inf.getConclusion() != ConclusionInforme.FAVORABLE) {
                throw new IllegalStateException("No se puede formular resolución FAVORABLE si el informe técnico concluyó: " + inf.getConclusion());
            }
            // Comprobación de requisitos habilitantes en tiempo real
            ChecklistAdministrativoDto checklist = checklistAdministrativoService.generarChecklist(exp.getId());
            if (!checklist.isAptoParaInformeFavorable()) {
                throw new IllegalStateException("No se cumplen las condiciones habilitantes del checklist administrativo en tiempo real: " + checklist.getResumen());
            }
        }

        // Verificar que no exista ya una resolución activa o firmada
        resolucionRepository.findByExpedienteIdAndEsVersionActualTrue(exp.getId())
                .ifPresent(resExistente -> {
                    if (resExistente.getEstado() == EstadoResolucion.FIRMADA) {
                        throw new IllegalStateException("Ya existe una resolución FIRMADA y vigente para este expediente: " + resExistente.getNumeroResolucion());
                    }
                    if (resExistente.getEstado() == EstadoResolucion.BORRADOR) {
                        throw new IllegalStateException("Ya existe un borrador de resolución en curso (" + resExistente.getNumeroResolucion() + "). Edite el borrador existente.");
                    }
                });

        String numeroResolucion = secuenciaResolucionService.generarSiguienteNumeroResolucion();
        int version = (int) (resolucionRepository.countByExpedienteId(exp.getId()) + 1);

        ResolucionAdministrativa resolucion = ResolucionAdministrativa.builder()
                .id(UUID.randomUUID())
                .numeroResolucion(numeroResolucion)
                .expediente(exp)
                .informeAdministrativo(inf)
                .versionInforme(inf.getVersion())
                .tipoDecision(dto.getTipoDecision())
                .estado(EstadoResolucion.BORRADOR)
                .fundamentos(dto.getFundamentos().trim())
                .referenciasLegales(dto.getReferenciasLegales() != null ? dto.getReferenciasLegales().trim() : null)
                .parteResolutiva(dto.getParteResolutiva() != null ? dto.getParteResolutiva().trim() : null)
                .esVersionActual(true)
                .version(version)
                .creadoPor(actor)
                .fechaCreacion(LocalDateTime.now())
                .build();

        // Generar hash preliminar de borrador
        byte[] pdfBorrador = resolucionPdfGenerator.generarResolucionPdf(resolucion);
        resolucion.setHashDocumento(ResolucionPdfGenerator.calcularHashSha256(pdfBorrador));

        ResolucionAdministrativa guardada = resolucionRepository.save(resolucion);

        registrarHistorial(guardada, "CREACION_BORRADOR", null, EstadoResolucion.BORRADOR, actor, "Borrador de resolución generado con correlativo " + numeroResolucion);

        return mapearDto(guardada);
    }

    @Transactional
    public ResolucionAdministrativaDto actualizarBorrador(UUID resolucionId, ActualizarResolucionDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";

        ResolucionAdministrativa res = resolucionRepository.findById(resolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Resolución no encontrada: " + resolucionId));

        if (res.getEstado() != EstadoResolucion.BORRADOR) {
            throw new IllegalStateException("Solo se permite editar resoluciones en estado BORRADOR. Estado actual: " + res.getEstado());
        }

        if (dto.getTipoDecision() != null) {
            if (dto.getTipoDecision() == TipoDecisionResolucion.FAVORABLE) {
                if (res.getExpediente().getEstado() != EstadoExpediente.APTO_PARA_RESOLUCION) {
                    throw new IllegalStateException("No se puede cambiar a FAVORABLE si el expediente no está en APTO_PARA_RESOLUCION");
                }
            }
            res.setTipoDecision(dto.getTipoDecision());
        }

        if (dto.getFundamentos() != null && !dto.getFundamentos().isBlank()) {
            res.setFundamentos(dto.getFundamentos().trim());
        }
        if (dto.getReferenciasLegales() != null) {
            res.setReferenciasLegales(dto.getReferenciasLegales().trim());
        }
        if (dto.getParteResolutiva() != null) {
            res.setParteResolutiva(dto.getParteResolutiva().trim());
        }

        res.setModificadoPor(actor);
        res.setFechaModificacion(LocalDateTime.now());

        // Actualizar hash del borrador
        byte[] pdf = resolucionPdfGenerator.generarResolucionPdf(res);
        res.setHashDocumento(ResolucionPdfGenerator.calcularHashSha256(pdf));

        ResolucionAdministrativa guardada = resolucionRepository.save(res);
        return mapearDto(guardada);
    }

    @Transactional
    public ResolucionAdministrativaDto aprobarResolucion(UUID resolucionId, AprobarResolucionDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        log.info("[RESOLUCION] Aprobando resolución {} por {}", resolucionId, actor);

        ResolucionAdministrativa res = resolucionRepository.findByIdWithLock(resolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Resolución no encontrada: " + resolucionId));

        if (res.getEstado() == EstadoResolucion.APROBADA_PENDIENTE_FIRMA || res.getEstado() == EstadoResolucion.FIRMADA) {
            throw new IllegalStateException("La resolución ya ha sido aprobada previamente (" + res.getEstado() + ")");
        }

        if (res.getEstado() != EstadoResolucion.BORRADOR) {
            throw new IllegalStateException("Solo se pueden aprobar resoluciones en estado BORRADOR. Estado actual: " + res.getEstado());
        }

        // SEGREGACIÓN DE FUNCIONES: La autoridad que aprueba la resolución no puede haber formulado el informe administrativo técnico previo
        InformeAdministrativo inf = res.getInformeAdministrativo();
        UUID usuarioId = obtenerUsuarioId(auth);

        boolean esMismoEvaluador = (inf.getEvaluadorId() != null && inf.getEvaluadorId().equals(usuarioId))
                || (actor != null && actor.equalsIgnoreCase(inf.getEvaluadorNombre()));

        if (esMismoEvaluador) {
            throw new IllegalStateException("Segregación de funciones: El funcionario evaluador del informe técnico no puede aprobar el acto resolutivo");
        }

        // Re-verificar condiciones habilitantes si es FAVORABLE
        if (res.getTipoDecision() == TipoDecisionResolucion.FAVORABLE) {
            if (res.getExpediente().getEstado() != EstadoExpediente.APTO_PARA_RESOLUCION) {
                throw new IllegalStateException("El expediente debe mantenerse en APTO_PARA_RESOLUCION");
            }
            ChecklistAdministrativoDto checklist = checklistAdministrativoService.generarChecklist(res.getExpediente().getId());
            if (!checklist.isAptoParaInformeFavorable()) {
                throw new IllegalStateException("No se puede aprobar resolución FAVORABLE: se detectaron requisitos no cumplidos en tiempo real. " + checklist.getResumen());
            }
        }

        EstadoResolucion anterior = res.getEstado();
        res.setEstado(EstadoResolucion.APROBADA_PENDIENTE_FIRMA);
        res.setAutoridadCompetenteId(usuarioId);
        res.setAutoridadCompetenteNombre(actor);
        res.setAutoridadCompetenteCargo(dto != null && dto.getCargoAutoridad() != null ? dto.getCargoAutoridad() : "Gerente de Desarrollo Económico");
        res.setFechaAprobacion(LocalDateTime.now());
        res.setModificadoPor(actor);
        res.setFechaModificacion(LocalDateTime.now());

        // Generar PDF y sellar hash pre-firma
        byte[] pdfAprobado = resolucionPdfGenerator.generarResolucionPdf(res);
        res.setHashDocumento(ResolucionPdfGenerator.calcularHashSha256(pdfAprobado));

        ResolucionAdministrativa guardada = resolucionRepository.save(res);

        registrarHistorial(guardada, "APROBACION_RESOLUCION", anterior, EstadoResolucion.APROBADA_PENDIENTE_FIRMA, actor,
                dto != null && dto.getObservaciones() != null ? dto.getObservaciones() : "Aprobación formal de resolución gerencial");

        return mapearDto(guardada);
    }

    @Transactional
    public ResolucionAdministrativaDto solicitarFirmaDigital(UUID resolucionId, SolicitarFirmaDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        log.info("[RESOLUCION] Solicitando firma digital para resolución {} por {}", resolucionId, actor);

        ResolucionAdministrativa res = resolucionRepository.findByIdWithLock(resolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Resolución no encontrada: " + resolucionId));

        if (res.getEstado() == EstadoResolucion.FIRMADA) {
            log.info("[RESOLUCION] Solicitud de firma idempotente: resolución {} ya se encuentra FIRMADA", resolucionId);
            return mapearDto(res);
        }

        if (res.getEstado() != EstadoResolucion.APROBADA_PENDIENTE_FIRMA && res.getEstado() != EstadoResolucion.ERROR_FIRMA) {
            throw new IllegalStateException("Para firmar, la resolución debe estar APROBADA_PENDIENTE_FIRMA o en ERROR_FIRMA. Estado actual: " + res.getEstado());
        }

        // Regla absoluta: Si el servicio de firma no está disponible, no simular firma ni emitir licencia
        if (!firmaDigitalPort.estaDisponible()) {
            res.setEstado(EstadoResolucion.APROBADA_PENDIENTE_FIRMA);
            res.setObservacionesFirma("Servicio institucional de firma digital no disponible o no configurado en este entorno. Resolución retenida (Ley N° 27269).");
            resolucionRepository.save(res);

            registrarHistorial(res, "INTENTO_FIRMA_SIN_SERVICIO", res.getEstado(), EstadoResolucion.APROBADA_PENDIENTE_FIRMA, actor,
                    "Intento de firma rechazado por falta de proveedor institucional activo");

            throw new IllegalStateException(
                    "El servicio institucional de firma digital no se encuentra disponible. "
                    + "La resolución permanece en estado APROBADA_PENDIENTE_FIRMA y no se emitirá licencia oficial (Ley N° 27269)."
            );
        }

        // Generar documento pre-firma
        byte[] pdfOriginal = resolucionPdfGenerator.generarResolucionPdf(res);
        String motivo = (dto != null && dto.getMotivoFirma() != null) ? dto.getMotivoFirma() : "Resolución de Licencia de Funcionamiento";
        String cargo = (res.getAutoridadCompetenteCargo() != null) ? res.getAutoridadCompetenteCargo() : "Gerente de Desarrollo Económico";

        String idTransaccion = (dto != null && dto.getIdTransaccionRemota() != null)
                ? dto.getIdTransaccionRemota()
                : UUID.randomUUID().toString();

        pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.OperacionFirma opFirma =
                pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.OperacionFirma.builder()
                        .id(UUID.randomUUID())
                        .resolucion(res)
                        .idTransaccionRemota(idTransaccion)
                        .estado(pe.gob.munihuamanga.licencias.common.enums.EstadoOperacionFirma.INICIADA)
                        .hashPreFirma(ResolucionPdfGenerator.calcularHashSha256(pdfOriginal))
                        .intentos(1)
                        .fechaInicio(LocalDateTime.now())
                        .creadoPor(actor)
                        .build();
        operacionFirmaRepository.save(opFirma);

        try {
            res.setEstado(EstadoResolucion.EN_PROCESO_FIRMA);
            resolucionRepository.saveAndFlush(res);

            ResultadoFirmaDigital resultado = firmaDigitalPort.firmarResolucion(
                    pdfOriginal,
                    res.getNumeroResolucion(),
                    motivo,
                    cargo
            );

            if (resultado == null || !resultado.isEsValida()) {
                res.setEstado(EstadoResolucion.ERROR_FIRMA);
                res.setObservacionesFirma(resultado != null ? resultado.getDetalle() : "Error desconocido en el proveedor de firma digital");
                resolucionRepository.save(res);

                opFirma.setEstado(pe.gob.munihuamanga.licencias.common.enums.EstadoOperacionFirma.FALLIDA);
                opFirma.setMensajeError(res.getObservacionesFirma());
                opFirma.setFechaFin(LocalDateTime.now());
                operacionFirmaRepository.save(opFirma);

                registrarHistorial(res, "ERROR_FIRMA_DIGITAL", EstadoResolucion.EN_PROCESO_FIRMA, EstadoResolucion.ERROR_FIRMA, actor, res.getObservacionesFirma());
                throw new IllegalStateException("Error o rechazo en la firma digital: " + res.getObservacionesFirma());
            }

            EstadoResolucion anterior = res.getEstado();
            res.setEstado(EstadoResolucion.FIRMADA);
            res.setFechaFirma(resultado.getFechaFirma() != null ? resultado.getFechaFirma() : LocalDateTime.now());
            res.setFirmanteNombre(resultado.getNombreFirmante() != null ? resultado.getNombreFirmante() : actor);
            res.setFirmanteCargo(resultado.getCargoFirmante() != null ? resultado.getCargoFirmante() : cargo);
            res.setFirmanteEntidad(resultado.getEntidadCertificadora() != null ? resultado.getEntidadCertificadora() : "Municipalidad Provincial de Huamanga");
            res.setHashDocumento(resultado.getHashSha256());
            res.setArchivoPdf(resultado.getPdfFirmado() != null ? resultado.getPdfFirmado() : pdfOriginal);
            res.setObservacionesFirma("Firma digital institucional verificada conforme a la Ley N° 27269");

            ResolucionAdministrativa guardada = resolucionRepository.save(res);

            opFirma.setEstado(pe.gob.munihuamanga.licencias.common.enums.EstadoOperacionFirma.COMPLETADA);
            opFirma.setHashPostFirma(resultado.getHashSha256());
            opFirma.setFechaFin(LocalDateTime.now());
            operacionFirmaRepository.save(opFirma);

            registrarHistorial(guardada, "FIRMA_DIGITAL_EXITOSA", anterior, EstadoResolucion.FIRMADA, actor,
                    "Firma digital PKI aplicada exitosamente. Hash SHA-256: " + resultado.getHashSha256());

            return mapearDto(guardada);

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RESOLUCION] Excepción inesperada durante el proceso de firma digital para {}", resolucionId, e);
            res.setEstado(EstadoResolucion.ERROR_FIRMA);
            res.setObservacionesFirma("Falla técnica de comunicación con proveedor PKI: " + e.getMessage());
            resolucionRepository.save(res);

            opFirma.setEstado(pe.gob.munihuamanga.licencias.common.enums.EstadoOperacionFirma.FALLIDA);
            opFirma.setMensajeError(e.getMessage());
            opFirma.setFechaFin(LocalDateTime.now());
            operacionFirmaRepository.save(opFirma);

            registrarHistorial(res, "ERROR_FIRMA_TECNICA", EstadoResolucion.EN_PROCESO_FIRMA, EstadoResolucion.ERROR_FIRMA, actor, res.getObservacionesFirma());
            throw new IllegalStateException("Falla de comunicación con el servicio de firma digital: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public EstadoFirmaDto consultarEstadoFirma(UUID resolucionId) {
        ResolucionAdministrativa res = resolucionRepository.findById(resolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Resolución no encontrada: " + resolucionId));

        return EstadoFirmaDto.builder()
                .resolucionId(res.getId())
                .numeroResolucion(res.getNumeroResolucion())
                .estadoResolucion(res.getEstado())
                .servicioFirmaDisponible(firmaDigitalPort.estaDisponible())
                .mensaje(res.getObservacionesFirma())
                .firmanteNombre(res.getFirmanteNombre())
                .firmanteCargo(res.getFirmanteCargo())
                .fechaFirma(res.getFechaFirma())
                .hashDocumento(res.getHashDocumento())
                .build();
    }

    @Transactional(readOnly = true)
    public ResolucionAdministrativaDto obtenerPorId(UUID resolucionId) {
        ResolucionAdministrativa res = resolucionRepository.findById(resolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Resolución no encontrada: " + resolucionId));
        return mapearDto(res);
    }

    @Transactional(readOnly = true)
    public ResolucionAdministrativaDto obtenerActualPorExpediente(UUID expedienteId) {
        ResolucionAdministrativa res = resolucionRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe resolución vigente para el expediente: " + expedienteId));
        return mapearDto(res);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfResolucion(UUID resolucionId) {
        ResolucionAdministrativa res = resolucionRepository.findById(resolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Resolución no encontrada: " + resolucionId));
        if (res.getArchivoPdf() != null && res.getArchivoPdf().length > 0) {
            return res.getArchivoPdf();
        }
        return resolucionPdfGenerator.generarResolucionPdf(res);
    }

    private void registrarHistorial(ResolucionAdministrativa res, String accion, EstadoResolucion anterior, EstadoResolucion nuevo, String actor, String detalle) {
        HistorialResolucion hist = HistorialResolucion.builder()
                .id(UUID.randomUUID())
                .resolucion(res)
                .accion(accion)
                .estadoAnterior(anterior)
                .estadoNuevo(nuevo)
                .actor(actor)
                .motivoObservaciones(detalle)
                .fechaRegistro(LocalDateTime.now())
                .build();
        historialResolucionRepository.save(hist);
    }

    private UUID obtenerUsuarioId(Authentication auth) {
        if (auth == null) return null;
        return usuarioRepository.findByUsername(auth.getName())
                .map(Usuario::getId)
                .orElse(null);
    }

    public ResolucionAdministrativaDto mapearDto(ResolucionAdministrativa r) {
        List<HistorialResolucionDto> hist = historialResolucionRepository.findByResolucionIdOrderByFechaRegistroDesc(r.getId())
                .stream()
                .map(h -> HistorialResolucionDto.builder()
                        .id(h.getId())
                        .resolucionId(r.getId())
                        .accion(h.getAccion())
                        .estadoAnterior(h.getEstadoAnterior())
                        .estadoNuevo(h.getEstadoNuevo())
                        .actor(h.getActor())
                        .motivoObservaciones(h.getMotivoObservaciones())
                        .fechaRegistro(h.getFechaRegistro())
                        .build())
                .toList();

        return ResolucionAdministrativaDto.builder()
                .id(r.getId())
                .numeroResolucion(r.getNumeroResolucion())
                .expedienteId(r.getExpediente().getId())
                .numeroExpediente(r.getExpediente().getNumeroTramite())
                .informeAdministrativoId(r.getInformeAdministrativo().getId())
                .numeroInforme(r.getInformeAdministrativo().getNumeroInforme())
                .versionInforme(r.getVersionInforme())
                .tipoDecision(r.getTipoDecision())
                .estadoResolucion(r.getEstado())
                .fundamentos(r.getFundamentos())
                .referenciasLegales(r.getReferenciasLegales())
                .parteResolutiva(r.getParteResolutiva())
                .autoridadCompetenteId(r.getAutoridadCompetenteId())
                .autoridadCompetenteNombre(r.getAutoridadCompetenteNombre())
                .autoridadCompetenteCargo(r.getAutoridadCompetenteCargo())
                .fechaAprobacion(r.getFechaAprobacion())
                .fechaFirma(r.getFechaFirma())
                .firmanteNombre(r.getFirmanteNombre())
                .firmanteCargo(r.getFirmanteCargo())
                .firmanteEntidad(r.getFirmanteEntidad())
                .hashDocumento(r.getHashDocumento())
                .observacionesFirma(r.getObservacionesFirma())
                .esVersionActual(r.getEsVersionActual())
                .version(r.getVersion())
                .creadoPor(r.getCreadoPor())
                .fechaCreacion(r.getFechaCreacion())
                .historial(hist)
                .build();
    }
}
