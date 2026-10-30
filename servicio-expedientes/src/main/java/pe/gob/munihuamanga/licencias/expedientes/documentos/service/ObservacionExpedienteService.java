package pe.gob.munihuamanga.licencias.expedientes.documentos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.CrearObservacionDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservacionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.SubsanarObservacionesDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.TipoObservacion;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.common.exception.TransicionInvalidaException;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.ObservacionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.ObservacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ComandosExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CalculadoraPlazos;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CodigoPlazo;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ParametrosPlazoService;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.RolesInstitucionales;
import pe.gob.munihuamanga.licencias.expedientes.security.VerificadorTitularidad;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Observaciones formales al expediente y su subsanación (TUO de la Ley N° 27444, art. 136).
 * Las observaciones pueden formularse en la etapa de admisibilidad o durante la evaluación administrativa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObservacionExpedienteService {

    private final ObservacionExpedienteRepository observacionRepository;
    private final DocumentoVersionadoRepository documentoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ComandosExpediente comandosExpediente;
    private final VerificadorTitularidad verificadorTitularidad;
    private final CalculadoraPlazos calculadoraPlazos;
    private final ParametrosPlazoService parametrosPlazo;
    private final NotificacionEmailService notificacionEmailService;
    private final Clock reloj;

    /**
     * Formula una o más observaciones y pasa el expediente a OBSERVADO.
     * El plazo de subsanación por defecto depende de la etapa en que se formula la observación.
     */
    @Transactional
    public List<ObservacionExpedienteDto> formularObservaciones(UUID expedienteId, List<CrearObservacionDto> dtos) {
        Expediente expediente = obtenerExpediente(expedienteId);
        if (!EventoExpediente.SOLICITUD_OBSERVADA.admiteOrigen(expediente.getEstado())) {
            throw new TransicionInvalidaException("No se pueden formular observaciones al expediente "
                    + expediente.getNumeroTramite() + " en estado " + expediente.getEstado() + ".");
        }

        Actor actor = verificadorTitularidad.actorActual();
        boolean enAdmisibilidad = expediente.getEstado() == EstadoExpediente.PRESENTADO;
        int plazoPorDefecto = parametrosPlazo.dias(enAdmisibilidad
                ? CodigoPlazo.SUBSANACION_ADMISIBILIDAD
                : CodigoPlazo.SUBSANACION_EVALUACION);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        List<ObservacionExpediente> guardadas = new ArrayList<>();
        List<String> motivos = new ArrayList<>();
        for (CrearObservacionDto dto : dtos) {
            TipoObservacion tipo = dto.getTipoObservacion() != null
                    ? dto.getTipoObservacion()
                    : (enAdmisibilidad ? TipoObservacion.RECEPCION_DOCUMENTAL : TipoObservacion.EVALUACION_ADMINISTRATIVA);
            int plazo = (dto.getPlazoDiasHabiles() != null && dto.getPlazoDiasHabiles() > 0) ? dto.getPlazoDiasHabiles() : plazoPorDefecto;
            String fundamento = (dto.getFundamentoLegal() != null && !dto.getFundamentoLegal().isBlank())
                    ? dto.getFundamentoLegal()
                    : tipo.getBaseLegalDefault();

            guardadas.add(observacionRepository.save(ObservacionExpediente.builder()
                    .id(UUID.randomUUID())
                    .expedienteId(expedienteId)
                    .documentoId(dto.getDocumentoId())
                    .tipoObservacion(tipo)
                    .requisitoAfectado(dto.getRequisitoAfectado())
                    .motivo(dto.getMotivo())
                    .fundamentoLegal(fundamento)
                    .fechaObservacion(ahora)
                    .observadorId(actor.usuarioId())
                    .observadorUsername(actor.nombreParaRegistro())
                    .plazoDiasHabiles(plazo)
                    .fechaLimiteSubsanacion(calculadoraPlazos.sumarDiasHabiles(ahora.toLocalDate(), plazo).atTime(LocalTime.of(23, 59)))
                    .subsanada(false)
                    .build()));

            if (dto.getDocumentoId() != null) {
                documentoRepository.findById(dto.getDocumentoId()).ifPresent(documento -> {
                    documento.setEstadoRevision(EstadoRevisionDocumento.OBSERVADO);
                    documentoRepository.save(documento);
                });
            }
            motivos.add("[" + dto.getRequisitoAfectado() + "] " + dto.getMotivo());
        }

        String resumen = String.join(" | ", motivos);
        expediente.setMotivoObservacion(recortar(resumen));
        expediente.setFechaObservacion(ahora);
        expedienteRepository.save(expediente);

        comandosExpediente.registrar(expedienteId, EventoExpediente.SOLICITUD_OBSERVADA, "Observaciones formuladas: " + resumen);
        notificacionEmailService.notificarObservacion(expediente, resumen);

        log.info("[OBSERVACIONES] {} observaciones registradas para el expediente {}.", guardadas.size(), expediente.getNumeroTramite());
        return guardadas.stream().map(this::mapearDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ObservacionExpedienteDto> listarObservaciones(UUID expedienteId) {
        Expediente expediente = obtenerExpediente(expedienteId);
        verificadorTitularidad.exigir(expediente, RolesInstitucionales.CONSULTA_EXPEDIENTES, PermisoOperador.CONSULTAR_EXPEDIENTE);
        return observacionRepository.findByExpedienteIdOrderByFechaObservacionDesc(expedienteId).stream()
                .map(this::mapearDto)
                .toList();
    }

    /**
     * Subsanación presentada por el administrado. Exige atender la totalidad de las observaciones
     * y, para las observaciones documentales, haber adjuntado una nueva versión del documento.
     */
    @Transactional
    public void subsanarObservaciones(UUID expedienteId, SubsanarObservacionesDto dto) {
        Expediente expediente = obtenerExpediente(expedienteId);
        if (expediente.getEstado() != EstadoExpediente.OBSERVADO) {
            throw new TransicionInvalidaException("Solo se pueden subsanar expedientes en estado OBSERVADO. Estado actual: " + expediente.getEstado());
        }
        verificadorTitularidad.exigir(expediente, RolesInstitucionales.ATENCION_PRESENCIAL, PermisoOperador.SUBSANAR_EXPEDIENTE);

        List<ObservacionExpediente> aProcesar = (dto.getObservacionesIds() != null && !dto.getObservacionesIds().isEmpty())
                ? dto.getObservacionesIds().stream().map(id -> obtenerObservacionDelExpediente(id, expedienteId)).toList()
                : observacionRepository.findByExpedienteIdAndSubsanadaFalse(expedienteId);

        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (ObservacionExpediente observacion : aProcesar) {
            vincularDocumentoSubsanado(observacion, expedienteId);
            observacion.setSubsanada(true);
            observacion.setFechaSubsanacion(ahora);
            observacion.setDetalleSubsanacion(dto.getDetalleSubsanacion());
            observacionRepository.save(observacion);
        }

        if (observacionRepository.existsByExpedienteIdAndSubsanadaFalse(expedienteId)) {
            throw new IllegalStateException("Quedan observaciones pendientes. El expediente pasa a SUBSANADO solo cuando se atienden todas las observaciones.");
        }

        expediente.setDetalleSubsanacion(recortar(dto.getDetalleSubsanacion()));
        expediente.setFechaSubsanacion(ahora);
        expedienteRepository.save(expediente);

        comandosExpediente.registrar(expedienteId, EventoExpediente.OBSERVACIONES_SUBSANADAS,
                "Subsanación de observaciones (TUO LPAG, art. 136): " + dto.getDetalleSubsanacion());
    }

    /**
     * Verificación de la subsanación por el funcionario: el expediente retorna a la etapa en que fue observado.
     */
    @Transactional
    public void reanudarRevision(UUID expedienteId, String sustento) {
        comandosExpediente.registrar(expedienteId, EventoExpediente.REVISION_REANUDADA, sustento);
    }

    private void vincularDocumentoSubsanado(ObservacionExpediente observacion, UUID expedienteId) {
        if (observacion.getDocumentoId() == null || observacion.getDocumentoSubsanacionId() != null) {
            return;
        }
        DocumentoVersionado original = documentoRepository.findById(observacion.getDocumentoId()).orElse(null);
        if (original == null) {
            return;
        }
        Optional<DocumentoVersionado> reciente = documentoRepository
                .findFirstByExpedienteIdAndTipoDocumentoOrderByVersionDesc(expedienteId, original.getTipoDocumento());
        if (reciente.isEmpty() || reciente.get().getVersion() <= original.getVersion()) {
            throw new IllegalStateException("No se puede subsanar la observación sobre " + observacion.getRequisitoAfectado()
                    + ": aún no se ha adjuntado una nueva versión corregida del documento.");
        }
        observacion.setDocumentoSubsanacionId(reciente.get().getId());
    }

    private ObservacionExpediente obtenerObservacionDelExpediente(UUID observacionId, UUID expedienteId) {
        ObservacionExpediente observacion = observacionRepository.findById(observacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Observación", observacionId));
        if (!observacion.getExpedienteId().equals(expedienteId)) {
            throw new IllegalArgumentException("La observación " + observacionId + " no pertenece al expediente indicado.");
        }
        return observacion;
    }

    private Expediente obtenerExpediente(UUID expedienteId) {
        return expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", expedienteId));
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= 500 ? texto : texto.substring(0, 500);
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
