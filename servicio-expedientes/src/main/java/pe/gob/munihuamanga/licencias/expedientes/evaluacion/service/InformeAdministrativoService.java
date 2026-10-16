package pe.gob.munihuamanga.licencias.expedientes.evaluacion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarInformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.AprobarInformeDto;
import pe.gob.munihuamanga.licencias.common.dto.ChecklistAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearInformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialDecisionInformeDto;
import pe.gob.munihuamanga.licencias.common.dto.InformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.RevisarInformeDto;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.HistorialInformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.HistorialInformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión integral de Informes Administrativos Técnicos (Fases 3, 4 y 5 - M08).
 * Asegura segregación de funciones, versionado inmutable, bloqueos y consistencia de estados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InformeAdministrativoService {

    private final InformeAdministrativoRepository informeAdministrativoRepository;
    private final HistorialInformeAdministrativoRepository historialInformeAdministrativoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ChecklistAdministrativoService checklistAdministrativoService;
    private final InformePdfGenerator informePdfGenerator;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;
    private final SecuenciaInformeService secuenciaInformeService;

    @Transactional
    public InformeAdministrativoDto elaborarInforme(CrearInformeAdministrativoDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        log.info("[INFORME] Iniciando elaboración de informe para expediente {} por {}", dto.getExpedienteId(), actor);

        Expediente exp = expedienteRepository.findById(dto.getExpedienteId())
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + dto.getExpedienteId()));

        // Ejecutar Checklist Administrativo Integral
        ChecklistAdministrativoDto checklist = checklistAdministrativoService.generarChecklist(exp.getId());

        // BLOQUEO: Si la conclusión es FAVORABLE, todos los requisitos deben cumplirse
        if (dto.getConclusion() == ConclusionInforme.FAVORABLE && !checklist.isAptoParaInformeFavorable()) {
            throw new IllegalStateException("No se puede emitir conclusión FAVORABLE: el checklist administrativo no se encuentra cumplido. " + checklist.getResumen());
        }

        // Manejo de versionado
        Optional<InformeAdministrativo> actualOpt = informeAdministrativoRepository
                .findByExpedienteIdAndEsVersionActualTrue(exp.getId());

        int version = 1;
        UUID versionAnteriorId = null;
        String numeroInforme;

        int anio = Year.now().getValue();
        if (actualOpt.isPresent()) {
            InformeAdministrativo actual = actualOpt.get();
            if (actual.getEstado() == EstadoInforme.APROBADO) {
                // Si ya estaba aprobado, se crea una nueva versión vinculada
                actual.setEsVersionActual(false);
                informeAdministrativoRepository.save(actual);
                version = actual.getVersion() + 1;
                versionAnteriorId = actual.getId();
                numeroInforme = actual.getNumeroInforme();
            } else {
                // Si estaba en borrador u observado, reutilizamos y versionamos
                actual.setEsVersionActual(false);
                informeAdministrativoRepository.save(actual);
                version = actual.getVersion() + 1;
                versionAnteriorId = actual.getId();
                numeroInforme = actual.getNumeroInforme();
            }
        } else {
            numeroInforme = secuenciaInformeService.generarSiguienteNumeroInforme();
        }

        String checklistJson = serializarChecklist(checklist);
        UUID evaluadorId = obtenerUsuarioId(auth);

        InformeAdministrativo informe = InformeAdministrativo.builder()
                .id(UUID.randomUUID())
                .expediente(exp)
                .numeroInforme(numeroInforme)
                .version(version)
                .evaluadorId(evaluadorId)
                .evaluadorNombre(actor)
                .fechaElaboracion(LocalDateTime.now())
                .conclusion(dto.getConclusion())
                .estado(EstadoInforme.BORRADOR)
                .fundamentosAdministrativos(dto.getFundamentosAdministrativos().trim())
                .referenciasDocumentales(dto.getReferenciasDocumentales().trim())
                .resultadosChecklistJson(checklistJson)
                .observacionesRevision(dto.getObservaciones())
                .esVersionActual(true)
                .versionAnteriorId(versionAnteriorId)
                .build();

        InformeAdministrativo guardado = informeAdministrativoRepository.save(informe);

        // Registrar Historial Inmutable
        registrarHistorial(guardado, "ELABORACION_INFORME", null, EstadoInforme.BORRADOR, actor,
                "Elaboración de informe administrativo con conclusión " + dto.getConclusion());

        // Actualizar estado del expediente si estaba presentado o subsanado
        if (exp.getEstado() == EstadoExpediente.PRESENTADO || exp.getEstado() == EstadoExpediente.SUBSANADO || exp.getEstado() == EstadoExpediente.DOCUMENTOS_VALIDADOS) {
            EstadoExpediente anterior = exp.getEstado();
            exp.setEstado(EstadoExpediente.EN_EVALUACION);
            expedienteRepository.save(exp);
            auditoriaService.registrarTransicion(exp.getId(), anterior, EstadoExpediente.EN_EVALUACION, actor,
                    "Inicio formal de evaluación administrativa técnica");
        }

        return mapearDto(guardado);
    }

    @Transactional
    public InformeAdministrativoDto actualizarBorrador(UUID informeId, ActualizarInformeAdministrativoDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        InformeAdministrativo inf = informeAdministrativoRepository.findById(informeId)
                .orElseThrow(() -> new IllegalArgumentException("Informe no encontrado: " + informeId));

        if (inf.getEstado() != EstadoInforme.BORRADOR && inf.getEstado() != EstadoInforme.OBSERVADO) {
            throw new IllegalStateException("Solo se pueden modificar informes en estado BORRADOR u OBSERVADO");
        }

        // Si cambia conclusión a FAVORABLE, validar checklist
        if (dto.getConclusion() == ConclusionInforme.FAVORABLE) {
            ChecklistAdministrativoDto checklist = checklistAdministrativoService.generarChecklist(inf.getExpediente().getId());
            if (!checklist.isAptoParaInformeFavorable()) {
                throw new IllegalStateException("No se puede emitir conclusión FAVORABLE: el checklist no se encuentra cumplido");
            }
            inf.setResultadosChecklistJson(serializarChecklist(checklist));
        }

        inf.setConclusion(dto.getConclusion());
        inf.setFundamentosAdministrativos(dto.getFundamentosAdministrativos().trim());
        inf.setReferenciasDocumentales(dto.getReferenciasDocumentales().trim());
        inf.setObservacionesRevision(dto.getObservaciones());

        InformeAdministrativo guardado = informeAdministrativoRepository.save(inf);
        registrarHistorial(guardado, "EDICION_BORRADOR", inf.getEstado(), inf.getEstado(), actor, "Actualización de fundamentos y conclusión");

        return mapearDto(guardado);
    }

    @Transactional
    public InformeAdministrativoDto enviarARevision(UUID informeId, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        InformeAdministrativo inf = informeAdministrativoRepository.findById(informeId)
                .orElseThrow(() -> new IllegalArgumentException("Informe no encontrado: " + informeId));

        if (inf.getEstado() != EstadoInforme.BORRADOR && inf.getEstado() != EstadoInforme.OBSERVADO) {
            throw new IllegalStateException("Solo se puede enviar a revisión un informe en estado BORRADOR u OBSERVADO");
        }

        EstadoInforme anterior = inf.getEstado();
        inf.setEstado(EstadoInforme.EN_REVISION);
        InformeAdministrativo guardado = informeAdministrativoRepository.save(inf);

        registrarHistorial(guardado, "ENVIO_A_REVISION", anterior, EstadoInforme.EN_REVISION, actor, "Envío de informe a revisión técnica");

        return mapearDto(guardado);
    }

    @Transactional
    public InformeAdministrativoDto revisarInforme(UUID informeId, RevisarInformeDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        InformeAdministrativo inf = informeAdministrativoRepository.findById(informeId)
                .orElseThrow(() -> new IllegalArgumentException("Informe no encontrado: " + informeId));

        if (inf.getEstado() != EstadoInforme.EN_REVISION) {
            throw new IllegalStateException("Solo se pueden revisar informes en estado EN_REVISION");
        }

        UUID revisorId = obtenerUsuarioId(auth);
        inf.setRevisorId(revisorId);
        inf.setRevisorNombre(actor);
        inf.setFechaRevision(LocalDateTime.now());
        inf.setObservacionesRevision(dto.getObservacionesRevision().trim());

        EstadoInforme anterior = inf.getEstado();
        EstadoInforme nuevo;

        if ("VISAR".equalsIgnoreCase(dto.getAccion())) {
            nuevo = EstadoInforme.VISADO;
            inf.setEstado(nuevo);
            registrarHistorial(inf, "VISADO_TECNICO", anterior, nuevo, actor, dto.getObservacionesRevision());
        } else if ("DEVOLVER".equalsIgnoreCase(dto.getAccion())) {
            nuevo = EstadoInforme.OBSERVADO;
            inf.setEstado(nuevo);
            registrarHistorial(inf, "DEVOLUCION_OBSERVADO", anterior, nuevo, actor, dto.getObservacionesRevision());
        } else {
            throw new IllegalArgumentException("Acción de revisión no reconocida: " + dto.getAccion() + ". Use VISAR o DEVOLVER.");
        }

        InformeAdministrativo guardado = informeAdministrativoRepository.save(inf);
        return mapearDto(guardado);
    }

    @Transactional
    public InformeAdministrativoDto aprobarInforme(UUID informeId, AprobarInformeDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        log.info("[INFORME] Iniciando aprobación de informe {} por {}", informeId, actor);

        // Bloqueo pesimista para evitar concurrencia y doble aprobación
        InformeAdministrativo inf = informeAdministrativoRepository.findByIdWithLock(informeId)
                .orElseThrow(() -> new IllegalArgumentException("Informe no encontrado: " + informeId));

        if (inf.getEstado() == EstadoInforme.APROBADO) {
            throw new IllegalStateException("El informe administrativo ya se encuentra aprobado.");
        }

        if (inf.getEstado() != EstadoInforme.VISADO) {
            throw new IllegalStateException("El informe debe estar debidamente VISADO por el funcionario revisor para ser aprobado formalmente");
        }

        UUID aprobadorId = obtenerUsuarioId(auth);

        // Control de Segregación de Funciones: El evaluador no puede aprobar su propio informe bajo ninguna circunstancia
        boolean esMismoEvaluador = (inf.getEvaluadorId() != null && inf.getEvaluadorId().equals(aprobadorId))
                || (actor != null && actor.equalsIgnoreCase(inf.getEvaluadorNombre()));
        if (esMismoEvaluador) {
            throw new IllegalStateException("Segregación de funciones: El funcionario evaluador no puede aprobar su propio informe administrativo");
        }

        // Si la conclusión es FAVORABLE, re-verificar que el checklist siga cumpliéndose en tiempo real
        if (inf.getConclusion() == ConclusionInforme.FAVORABLE) {
            ChecklistAdministrativoDto checklist = checklistAdministrativoService.generarChecklist(inf.getExpediente().getId());
            if (!checklist.isAptoParaInformeFavorable()) {
                throw new IllegalStateException("No se puede aprobar informe con conclusión FAVORABLE: se detectaron requisitos no cumplidos en tiempo real. " + checklist.getResumen());
            }
        }

        EstadoInforme anterior = inf.getEstado();
        inf.setEstado(EstadoInforme.APROBADO);
        inf.setAprobadorId(aprobadorId);
        inf.setAprobadorNombre(actor);
        inf.setFechaAprobacion(LocalDateTime.now());

        InformeAdministrativo guardado = informeAdministrativoRepository.save(inf);

        registrarHistorial(guardado, "APROBACION_ADMINISTRATIVA", anterior, EstadoInforme.APROBADO, actor, dto.getObservacionesAprobacion());

        // Actualizar estado del expediente según conclusión
        Expediente exp = inf.getExpediente();
        EstadoExpediente estExpAnterior = exp.getEstado();

        if (inf.getConclusion() == ConclusionInforme.FAVORABLE) {
            exp.setEstado(EstadoExpediente.APTO_PARA_RESOLUCION);
            expedienteRepository.save(exp);
            auditoriaService.registrarTransicion(exp.getId(), estExpAnterior, EstadoExpediente.APTO_PARA_RESOLUCION, actor,
                    "Aprobación de Informe Administrativo Favorable " + inf.getNumeroInforme());
        } else if (inf.getConclusion() == ConclusionInforme.DESFAVORABLE) {
            exp.setEstado(EstadoExpediente.RECHAZADO);
            expedienteRepository.save(exp);
            auditoriaService.registrarTransicion(exp.getId(), estExpAnterior, EstadoExpediente.RECHAZADO, actor,
                    "Informe Administrativo Desfavorable " + inf.getNumeroInforme());
        } else if (inf.getConclusion() == ConclusionInforme.REQUIERE_SUBSANACION) {
            exp.setEstado(EstadoExpediente.OBSERVADO);
            expedienteRepository.save(exp);
            auditoriaService.registrarTransicion(exp.getId(), estExpAnterior, EstadoExpediente.OBSERVADO, actor,
                    "Informe Administrativo con requerimiento de subsanación " + inf.getNumeroInforme());
        }

        return mapearDto(guardado);
    }

    @Transactional(readOnly = true)
    public InformeAdministrativoDto obtenerInformeActualPorExpediente(UUID expedienteId) {
        InformeAdministrativo inf = informeAdministrativoRepository
                .findByExpedienteIdAndEsVersionActualTrue(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe informe administrativo registrado para el expediente " + expedienteId));
        return mapearDto(inf);
    }

    @Transactional(readOnly = true)
    public InformeAdministrativoDto obtenerPorId(UUID id) {
        InformeAdministrativo inf = informeAdministrativoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Informe no encontrado: " + id));
        return mapearDto(inf);
    }

    @Transactional(readOnly = true)
    public List<HistorialDecisionInformeDto> listarHistorial(UUID informeId) {
        return historialInformeAdministrativoRepository.findByInformeIdOrderByFechaAsc(informeId).stream()
                .map(this::mapearHistorialDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] descargarPdfPreliminar(UUID informeId) {
        InformeAdministrativo inf = informeAdministrativoRepository.findById(informeId)
                .orElseThrow(() -> new IllegalArgumentException("Informe no encontrado: " + informeId));

        ChecklistAdministrativoDto checklist = checklistAdministrativoService.generarChecklist(inf.getExpediente().getId());
        return informePdfGenerator.generarPdf(inf, checklist);
    }

    private void registrarHistorial(InformeAdministrativo informe, String accion, EstadoInforme ant, EstadoInforme nuevo, String actor, String motivo) {
        HistorialInformeAdministrativo hist = HistorialInformeAdministrativo.builder()
                .id(UUID.randomUUID())
                .informe(informe)
                .accion(accion)
                .estadoAnterior(ant)
                .estadoNuevo(nuevo)
                .responsable(actor)
                .fecha(LocalDateTime.now())
                .motivo(motivo)
                .build();
        historialInformeAdministrativoRepository.save(hist);
    }

    private String serializarChecklist(ChecklistAdministrativoDto checklist) {
        try {
            return objectMapper.writeValueAsString(checklist);
        } catch (Exception e) {
            log.warn("No se pudo serializar checklist: {}", e.getMessage());
            return "{}";
        }
    }

    private UUID obtenerUsuarioId(Authentication auth) {
        // En un entorno de producción se obtiene del Principal (CustomUserDetails); en fallback se genera un UUID determinista
        if (auth != null && auth.getName() != null) {
            return UUID.nameUUIDFromBytes(auth.getName().getBytes());
        }
        return UUID.randomUUID();
    }

    private InformeAdministrativoDto mapearDto(InformeAdministrativo inf) {
        List<HistorialDecisionInformeDto> hist = historialInformeAdministrativoRepository
                .findByInformeIdOrderByFechaAsc(inf.getId()).stream()
                .map(this::mapearHistorialDto)
                .collect(Collectors.toList());

        return InformeAdministrativoDto.builder()
                .id(inf.getId())
                .expedienteId(inf.getExpediente().getId())
                .numeroExpediente(inf.getExpediente().getNumeroTramite())
                .numeroInforme(inf.getNumeroInforme())
                .version(inf.getVersion())
                .evaluadorId(inf.getEvaluadorId())
                .evaluadorNombre(inf.getEvaluadorNombre())
                .revisorId(inf.getRevisorId())
                .revisorNombre(inf.getRevisorNombre())
                .aprobadorId(inf.getAprobadorId())
                .aprobadorNombre(inf.getAprobadorNombre())
                .fechaElaboracion(inf.getFechaElaboracion())
                .fechaRevision(inf.getFechaRevision())
                .fechaAprobacion(inf.getFechaAprobacion())
                .conclusion(inf.getConclusion())
                .estado(inf.getEstado())
                .fundamentosAdministrativos(inf.getFundamentosAdministrativos())
                .referenciasDocumentales(inf.getReferenciasDocumentales())
                .resultadosChecklistJson(inf.getResultadosChecklistJson())
                .observacionesRevision(inf.getObservacionesRevision())
                .esVersionActual(inf.getEsVersionActual())
                .versionAnteriorId(inf.getVersionAnteriorId())
                .historial(hist)
                .build();
    }

    private HistorialDecisionInformeDto mapearHistorialDto(HistorialInformeAdministrativo h) {
        return HistorialDecisionInformeDto.builder()
                .id(h.getId())
                .informeId(h.getInforme().getId())
                .accion(h.getAccion())
                .estadoAnterior(h.getEstadoAnterior())
                .estadoNuevo(h.getEstadoNuevo())
                .responsable(h.getResponsable())
                .fecha(h.getFecha())
                .motivo(h.getMotivo())
                .build();
    }
}
