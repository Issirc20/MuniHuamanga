package pe.gob.munihuamanga.licencias.expedientes.documentos.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoVersionadoDto;
import pe.gob.munihuamanga.licencias.common.dto.DocumentoVersionadoDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.DocumentoBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.DocumentoBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.security.DocumentoSecurityValidator;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.ObservacionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.ObservacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.RolesInstitucionales;
import pe.gob.munihuamanga.licencias.expedientes.security.VerificadorTitularidad;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gestión documental con versionado inmutable y verificación de integridad SHA-256.
 * El administrado solo puede adjuntar documentos en las etapas que lo requieren:
 * anexos ITSE durante la etapa ITSE, comprobante durante el pago y nuevas versiones durante la subsanación.
 */
@Slf4j
@Service
public class DocumentoVersionadoService {

    private static final Set<TipoDocumentoAdjunto> ANEXOS_ITSE = Arrays.stream(TipoAnexoItse.values())
            .map(TipoAnexoItse::getTipoDocumento)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoDocumentoAdjunto.class)));

    private static final RolUsuario[] FUNCIONARIOS_QUE_ADJUNTAN = {RolUsuario.ROLE_MESA_DE_PARTES, RolUsuario.ROLE_EVALUADOR};

    /** Identificador registrado cuando la carga la realiza un proceso sin persona autenticada. */
    private static final UUID SIN_USUARIO = new UUID(0L, 0L);

    private final DocumentoVersionadoRepository documentoRepository;
    private final DocumentoBorradorRepository documentoBorradorRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ObservacionExpedienteRepository observacionRepository;
    private final DocumentoSecurityValidator securityValidator;
    private final VerificadorTitularidad verificadorTitularidad;

    // Almacenamiento binario en memoria: será sustituido por el repositorio documental en el módulo de documentos.
    private final Map<UUID, byte[]> almacenamientoBinario = new ConcurrentHashMap<>();

    public DocumentoVersionadoService(DocumentoVersionadoRepository documentoRepository,
                                     DocumentoBorradorRepository documentoBorradorRepository,
                                     ExpedienteRepository expedienteRepository,
                                     ObservacionExpedienteRepository observacionRepository,
                                     DocumentoSecurityValidator securityValidator,
                                     VerificadorTitularidad verificadorTitularidad) {
        this.documentoRepository = documentoRepository;
        this.documentoBorradorRepository = documentoBorradorRepository;
        this.expedienteRepository = expedienteRepository;
        this.observacionRepository = observacionRepository;
        this.securityValidator = securityValidator;
        this.verificadorTitularidad = verificadorTitularidad;
    }

    /**
     * Adjunta una nueva versión de un documento al expediente. Nunca sobrescribe versiones existentes.
     */
    @Transactional
    public DocumentoVersionadoDto adjuntarDocumentoAExpediente(UUID expedienteId, AdjuntarDocumentoVersionadoDto dto) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", expedienteId));

        EstadoExpediente estado = expediente.getEstado();
        if (estado.estaResuelto()) {
            throw new IllegalStateException("No se pueden agregar documentos a un expediente resuelto o concluido (estado " + estado + ").");
        }

        Actor actor = verificadorTitularidad.actorActual();
        if (!actor.tieneAlgunRol(FUNCIONARIOS_QUE_ADJUNTAN)) {
            if (!etapaAdmiteCargaDelAdministrado(estado, dto.getTipoDocumento())) {
                throw new IllegalStateException("En el estado " + estado + " el expediente no admite la carga del documento "
                        + dto.getTipoDocumento() + " por el administrado.");
            }
            verificadorTitularidad.exigir(expediente, RolesInstitucionales.ATENCION_PRESENCIAL,
                    PermisoOperador.SUBSANAR_EXPEDIENTE, PermisoOperador.PRESENTAR_EXPEDIENTE);
        }

        if (dto.getObservacionId() != null) {
            validarPertinenciaObservacion(dto, expedienteId);
        }

        byte[] contenido;
        try {
            contenido = Base64.getDecoder().decode(dto.getContenidoBase64());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El contenido Base64 proporcionado es inválido.", ex);
        }

        String nombreSanitizado = securityValidator.validarYSanitizar(dto.getNombreArchivo(), dto.getTipoContenido(), contenido);
        String hashSha256 = securityValidator.calcularSha256(contenido);

        Optional<DocumentoVersionado> anterior = documentoRepository
                .findFirstByExpedienteIdAndTipoDocumentoOrderByVersionDesc(expedienteId, dto.getTipoDocumento());
        int versionNueva = anterior.map(doc -> doc.getVersion() + 1).orElse(1);
        UUID versionAnteriorId = anterior.map(DocumentoVersionado::getId).orElse(null);
        anterior.ifPresent(doc -> doc.setEsVersionActual(false));
        documentoRepository.desmarcarVersionesActualesExpediente(expedienteId, dto.getTipoDocumento());

        UUID docId = UUID.randomUUID();
        DocumentoVersionado guardado = documentoRepository.save(DocumentoVersionado.builder()
                .id(docId)
                .expedienteId(expedienteId)
                .tipoDocumento(dto.getTipoDocumento())
                .version(versionNueva)
                .nombreArchivo(nombreSanitizado)
                .tipoContenido(dto.getTipoContenido() != null ? dto.getTipoContenido() : "application/pdf")
                .tamanioBytes((long) contenido.length)
                .hashSha256(hashSha256)
                .rutaAlmacenamiento("storage/expedientes/" + expedienteId + "/v" + versionNueva + "_" + nombreSanitizado)
                .fechaCarga(LocalDateTime.now())
                .cargadoPor(actor.usuarioId() != null ? actor.usuarioId() : SIN_USUARIO)
                .cargadoPorNombre(actor.nombreParaRegistro())
                .estadoRevision(EstadoRevisionDocumento.PENDIENTE)
                .versionAnteriorId(versionAnteriorId)
                .motivoActualizacion(dto.getMotivoActualizacion())
                .esVersionActual(true)
                .observacionId(dto.getObservacionId())
                .build());
        almacenamientoBinario.put(docId, contenido);

        if (dto.getObservacionId() != null) {
            observacionRepository.findById(dto.getObservacionId()).ifPresent(observacion -> {
                observacion.setDocumentoSubsanacionId(guardado.getId());
                observacionRepository.save(observacion);
            });
        }

        log.info("[DOCUMENTOS] Versión {} del documento {} registrada para el expediente {}.",
                guardado.getVersion(), guardado.getTipoDocumento(), expediente.getNumeroTramite());
        return mapearDto(guardado, false);
    }

    /**
     * Descarga el documento verificando su integridad antes de la entrega.
     */
    @Transactional(readOnly = true)
    public DocumentoVersionadoDto descargarDocumento(UUID documentoId) {
        DocumentoVersionado doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento versionado", documentoId));

        if (doc.getExpedienteId() != null) {
            Expediente expediente = expedienteRepository.findById(doc.getExpedienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", doc.getExpedienteId()));
            verificadorTitularidad.exigir(expediente, RolesInstitucionales.CONSULTA_EXPEDIENTES, PermisoOperador.CONSULTAR_EXPEDIENTE);
        }

        byte[] contenido = almacenamientoBinario.get(documentoId);
        if (contenido == null) {
            throw new RecursoNoEncontradoException("El contenido del documento no se encuentra disponible en el almacenamiento.");
        }
        securityValidator.verificarIntegridadSha256(contenido, doc.getHashSha256());

        DocumentoVersionadoDto dto = mapearDto(doc, true);
        dto.setContenidoBase64(Base64.getEncoder().encodeToString(contenido));
        return dto;
    }

    /**
     * Registra en memoria el contenido binario de un documento.
     */
    public void registrarContenidoBinario(UUID docId, byte[] contenido) {
        if (docId != null && contenido != null) {
            almacenamientoBinario.put(docId, contenido);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentoVersionadoDto> listarDocumentosExpediente(UUID expedienteId, boolean soloActuales) {
        List<DocumentoVersionado> lista = soloActuales
                ? documentoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)
                : documentoRepository.findByExpedienteIdOrderByTipoDocumentoAscVersionDesc(expedienteId);
        return lista.stream().map(d -> mapearDto(d, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentoVersionadoDto> listarHistorialVersiones(UUID expedienteId, TipoDocumentoAdjunto tipoDocumento) {
        return documentoRepository.findByExpedienteIdAndTipoDocumentoOrderByVersionDesc(expedienteId, tipoDocumento)
                .stream().map(d -> mapearDto(d, false)).collect(Collectors.toList());
    }

    /**
     * Migra los documentos adjuntos del borrador al expediente formalizado en su versión 1.
     */
    @Transactional
    public void migrarDocumentosBorradorAExpediente(UUID borradorId, UUID expedienteId, UUID usuarioId, String nombreUsuario) {
        List<DocumentoBorrador> docsBorrador = documentoBorradorRepository.findByBorradorId(borradorId);
        for (DocumentoBorrador db : docsBorrador) {
            documentoRepository.save(DocumentoVersionado.builder()
                    .id(UUID.randomUUID())
                    .expedienteId(expedienteId)
                    .borradorId(borradorId)
                    .tipoDocumento(db.getTipoDocumento())
                    .version(1)
                    .nombreArchivo(db.getNombreArchivo())
                    .tipoContenido(db.getTipoContenido())
                    .tamanioBytes(db.getTamanioBytes())
                    .hashSha256(db.getHashSha256())
                    .rutaAlmacenamiento("storage/expedientes/" + expedienteId + "/v1_" + db.getNombreArchivo())
                    .fechaCarga(LocalDateTime.now())
                    .cargadoPor(db.getCargadoPor() != null ? db.getCargadoPor() : (usuarioId != null ? usuarioId : SIN_USUARIO))
                    .cargadoPorNombre(nombreUsuario)
                    .estadoRevision(EstadoRevisionDocumento.PENDIENTE)
                    .esVersionActual(true)
                    .build());
        }
        log.info("[DOCUMENTOS] {} documentos del borrador incorporados al expediente {}.", docsBorrador.size(), expedienteId);
    }

    private void validarPertinenciaObservacion(AdjuntarDocumentoVersionadoDto dto, UUID expedienteId) {
        ObservacionExpediente observacion = observacionRepository.findById(dto.getObservacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Observación", dto.getObservacionId()));
        if (!observacion.getExpedienteId().equals(expedienteId)) {
            throw new IllegalArgumentException("La observación indicada no pertenece al expediente.");
        }
        if (observacion.getDocumentoId() != null) {
            documentoRepository.findById(observacion.getDocumentoId())
                    .filter(observado -> observado.getTipoDocumento() != dto.getTipoDocumento())
                    .ifPresent(observado -> {
                        throw new IllegalArgumentException("El tipo de documento adjuntado (" + dto.getTipoDocumento()
                                + ") no corresponde al documento observado (" + observado.getTipoDocumento() + ").");
                    });
        }
    }

    private static boolean etapaAdmiteCargaDelAdministrado(EstadoExpediente estado, TipoDocumentoAdjunto tipo) {
        return switch (estado) {
            case OBSERVADO -> true;
            case ITSE_EN_TRAMITE -> ANEXOS_ITSE.contains(tipo);
            case PENDIENTE_PAGO -> tipo == TipoDocumentoAdjunto.COMPROBANTE_PAGO_SAT;
            default -> false;
        };
    }

    private DocumentoVersionadoDto mapearDto(DocumentoVersionado doc, boolean incluirContenido) {
        return DocumentoVersionadoDto.builder()
                .id(doc.getId())
                .expedienteId(doc.getExpedienteId())
                .borradorId(doc.getBorradorId())
                .tipoDocumento(doc.getTipoDocumento())
                .version(doc.getVersion())
                .nombreArchivo(doc.getNombreArchivo())
                .tipoContenido(doc.getTipoContenido())
                .tamanioBytes(doc.getTamanioBytes())
                .hashSha256(doc.getHashSha256())
                .fechaCarga(doc.getFechaCarga())
                .cargadoPor(doc.getCargadoPor())
                .cargadoPorNombre(doc.getCargadoPorNombre())
                .estadoRevision(doc.getEstadoRevision())
                .versionAnteriorId(doc.getVersionAnteriorId())
                .motivoActualizacion(doc.getMotivoActualizacion())
                .esVersionActual(doc.getEsVersionActual())
                .observacionId(doc.getObservacionId())
                .build();
    }
}
