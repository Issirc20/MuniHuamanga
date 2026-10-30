package pe.gob.munihuamanga.licencias.expedientes.itse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.AnexoItseDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteItseDto;
import pe.gob.munihuamanga.licencias.common.dto.LiquidacionDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservacionItseDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservacionItseItemDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservarItseDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistrarCargaItseDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarItseDto;
import pe.gob.munihuamanga.licencias.common.enums.CanalRecepcion;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoItse;
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.common.exception.TransicionInvalidaException;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.itse.model.ExpedienteItse;
import pe.gob.munihuamanga.licencias.expedientes.itse.model.ObservacionItse;
import pe.gob.munihuamanga.licencias.expedientes.itse.repository.ExpedienteItseRepository;
import pe.gob.munihuamanga.licencias.expedientes.itse.repository.ObservacionItseRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ComandosExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.service.LiquidadorTasa;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.RolesInstitucionales;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestión de la etapa ITSE: carga de los cuatro anexos firmados por Defensa Civil,
 * verificación formal por la oficina de licencias y liquidación de la tasa según el nivel de riesgo oficial.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItseService {

    private final ExpedienteItseRepository itseRepository;
    private final ObservacionItseRepository observacionRepository;
    private final DocumentoVersionadoRepository documentoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ComandosExpediente comandosExpediente;
    private final ProveedorActor proveedorActor;
    private final LiquidadorTasa liquidadorTasa;
    private final List<VerificacionAnexoItse> verificaciones;
    private final NotificacionEmailService notificacionEmailService;
    private final Clock reloj;

    /** Abre la etapa ITSE del expediente. Es idempotente. */
    @Transactional
    public void iniciarEtapa(UUID expedienteId) {
        if (itseRepository.existsByExpedienteId(expedienteId)) {
            return;
        }
        itseRepository.save(ExpedienteItse.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedienteId)
                .estado(EstadoItse.PENDIENTE_CARGA)
                .fechaCreacion(LocalDateTime.now(reloj))
                .build());
    }

    @Transactional(readOnly = true)
    public ExpedienteItseDto obtener(UUID expedienteId) {
        Expediente expediente = obtenerExpediente(expedienteId);
        return aDto(expediente, obtenerEtapa(expedienteId));
    }

    @Transactional(readOnly = true)
    public List<ExpedienteItseDto> bandejaRevision() {
        return itseRepository.findByEstadoOrderByFechaCargaAsc(EstadoItse.EN_REVISION).stream()
                .map(etapa -> aDto(obtenerExpediente(etapa.getExpedienteId()), etapa))
                .toList();
    }

    /**
     * Registra la carga de los cuatro anexos firmados. Los archivos se adjuntan previamente como
     * documentos versionados del expediente; aquí se verifican y se envían a revisión.
     */
    @Transactional
    public ExpedienteItseDto registrarCarga(UUID expedienteId, RegistrarCargaItseDto dto) {
        Expediente expediente = obtenerExpedienteEnEstado(expedienteId, EstadoExpediente.ITSE_EN_TRAMITE);
        ExpedienteItse etapa = obtenerEtapa(expedienteId);

        List<String> hallazgos = verificarAnexos(expedienteId, etapa);
        if (!hallazgos.isEmpty()) {
            throw new IllegalStateException("Los anexos ITSE no pueden enviarse a revisión: " + String.join(" ", hallazgos));
        }

        Actor actor = proveedorActor.actual();
        LiquidacionDto referencial = liquidadorTasa.liquidar(dto.getNivelDeclarado(), false);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        etapa.setEstado(EstadoItse.EN_REVISION);
        etapa.setCanal(actor.tieneAlgunRol(RolesInstitucionales.ATENCION_PRESENCIAL) ? CanalRecepcion.PRESENCIAL : CanalRecepcion.VIRTUAL);
        etapa.setNivelDeclarado(dto.getNivelDeclarado());
        etapa.setCargadoPorId(actor.usuarioId());
        etapa.setCargadoPor(actor.nombreParaRegistro());
        etapa.setFechaCarga(ahora);
        etapa.setComentarioCarga(dto.getComentario());
        aplicarLiquidacion(etapa, referencial);
        itseRepository.save(etapa);

        observacionRepository.findByExpedienteItseIdAndAtendidaFalse(etapa.getId()).forEach(observacion -> {
            observacion.setAtendida(true);
            observacion.setFechaAtencion(ahora);
            observacionRepository.save(observacion);
        });

        comandosExpediente.registrar(expedienteId, EventoExpediente.ANEXOS_ITSE_CARGADOS,
                String.format("Carga de los cuatro anexos ITSE firmados (canal %s). Nivel de riesgo declarado: %s.",
                        etapa.getCanal(), dto.getNivelDeclarado()));
        return aDto(expediente, etapa);
    }

    @Transactional
    public ExpedienteItseDto observar(UUID expedienteId, ObservarItseDto dto) {
        Expediente expediente = obtenerExpedienteEnEstado(expedienteId, EstadoExpediente.ITSE_EN_REVISION);
        ExpedienteItse etapa = obtenerEtapa(expedienteId);
        Actor actor = proveedorActor.actual();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        for (ObservacionItseItemDto item : dto.getObservaciones()) {
            observacionRepository.save(ObservacionItse.builder()
                    .id(UUID.randomUUID())
                    .expedienteItseId(etapa.getId())
                    .anexo(item.getAnexo())
                    .tipo(item.getTipo())
                    .motivo(item.getMotivo().trim())
                    .observadoPorId(actor.usuarioId())
                    .observadoPor(actor.nombreParaRegistro())
                    .fechaObservacion(ahora)
                    .build());
        }

        etapa.setEstado(EstadoItse.OBSERVADO);
        itseRepository.save(etapa);

        String resumen = dto.getObservaciones().stream()
                .map(item -> item.getAnexo().getDescripcion() + ": " + item.getMotivo().trim())
                .collect(Collectors.joining(" | "));
        comandosExpediente.registrar(expedienteId, EventoExpediente.ITSE_OBSERVADO, "Anexos ITSE observados. " + resumen);
        notificacionEmailService.notificarObservacion(expediente, resumen);
        return aDto(expediente, etapa);
    }

    @Transactional
    public ExpedienteItseDto validar(UUID expedienteId, ValidarItseDto dto) {
        Expediente expediente = obtenerExpedienteEnEstado(expedienteId, EstadoExpediente.ITSE_EN_REVISION);
        ExpedienteItse etapa = obtenerEtapa(expedienteId);

        if (!(dto.isAnexosCompletosYLegibles() && dto.isFirmasYSellosConformes()
                && dto.isDatosCoincidenConSolicitud() && dto.isNivelRiesgoCoherente())) {
            throw new IllegalArgumentException(
                    "Para validar el ITSE deben declararse conformes todos los puntos de la lista de verificación. "
                            + "Si alguno no es conforme, corresponde formular una observación.");
        }

        Actor actor = proveedorActor.actual();
        LiquidacionDto definitiva = liquidadorTasa.liquidar(dto.getNivelOficial(), true);

        etapa.setEstado(EstadoItse.VALIDADO);
        etapa.setNivelOficial(dto.getNivelOficial());
        etapa.setValidadoPorId(actor.usuarioId());
        etapa.setValidadoPor(actor.nombreParaRegistro());
        etapa.setFechaValidacion(LocalDateTime.now(reloj));
        etapa.setComentarioValidacion(dto.getComentario());
        aplicarLiquidacion(etapa, definitiva);
        itseRepository.save(etapa);

        expediente.setNivelRiesgo(dto.getNivelOficial());
        expediente.setMontoTasa(definitiva.getMonto());
        expedienteRepository.save(expediente);

        comandosExpediente.registrar(expedienteId, EventoExpediente.ITSE_VALIDADO,
                String.format("ITSE validado. Nivel de riesgo oficial: %s. Liquidación: S/ %s.",
                        dto.getNivelOficial(), definitiva.getMonto()));
        return aDto(expediente, etapa);
    }

    @Transactional(readOnly = true)
    public Optional<LiquidacionDto> liquidacionDefinitiva(UUID expedienteId) {
        return itseRepository.findByExpedienteId(expedienteId)
                .filter(ExpedienteItse::isLiquidacionDefinitiva)
                .map(ItseService::liquidacionDe);
    }

    private List<String> verificarAnexos(UUID expedienteId, ExpedienteItse etapa) {
        List<String> hallazgos = new ArrayList<>();
        List<ObservacionItse> pendientes = observacionRepository.findByExpedienteItseIdAndAtendidaFalse(etapa.getId());

        for (TipoAnexoItse anexo : TipoAnexoItse.values()) {
            Optional<DocumentoVersionado> documento = ultimaVersion(expedienteId, anexo);
            verificaciones.forEach(verificacion -> verificacion.verificar(anexo, documento).ifPresent(hallazgos::add));

            pendientes.stream()
                    .filter(observacion -> observacion.getAnexo() == anexo)
                    .filter(observacion -> documento.isEmpty()
                            || !documento.get().getFechaCarga().isAfter(observacion.getFechaObservacion()))
                    .findFirst()
                    .ifPresent(observacion -> hallazgos.add(anexo.getDescripcion()
                            + ": debe cargarse una nueva versión que atienda la observación formulada."));
        }
        return hallazgos;
    }

    private Optional<DocumentoVersionado> ultimaVersion(UUID expedienteId, TipoAnexoItse anexo) {
        return documentoRepository.findFirstByExpedienteIdAndTipoDocumentoOrderByVersionDesc(expedienteId, anexo.getTipoDocumento());
    }

    private static void aplicarLiquidacion(ExpedienteItse etapa, LiquidacionDto liquidacion) {
        etapa.setMontoLiquidado(liquidacion.getMonto());
        etapa.setConceptoLiquidacion(liquidacion.getConcepto());
        etapa.setFechaLiquidacion(liquidacion.getFechaLiquidacion());
        etapa.setLiquidacionDefinitiva(liquidacion.isDefinitiva());
    }

    private static LiquidacionDto liquidacionDe(ExpedienteItse etapa) {
        if (etapa.getMontoLiquidado() == null) {
            return null;
        }
        return LiquidacionDto.builder()
                .nivelRiesgo(etapa.isLiquidacionDefinitiva() ? etapa.getNivelOficial() : etapa.getNivelDeclarado())
                .monto(etapa.getMontoLiquidado())
                .concepto(etapa.getConceptoLiquidacion())
                .fechaLiquidacion(etapa.getFechaLiquidacion())
                .definitiva(etapa.isLiquidacionDefinitiva())
                .build();
    }

    private Expediente obtenerExpediente(UUID expedienteId) {
        return expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", expedienteId));
    }

    private Expediente obtenerExpedienteEnEstado(UUID expedienteId, EstadoExpediente requerido) {
        Expediente expediente = obtenerExpediente(expedienteId);
        if (expediente.getEstado() != requerido) {
            throw new TransicionInvalidaException(String.format(
                    "La operación requiere que el expediente %s se encuentre en estado %s. Estado actual: %s.",
                    expediente.getNumeroTramite(), requerido, expediente.getEstado()));
        }
        return expediente;
    }

    private ExpedienteItse obtenerEtapa(UUID expedienteId) {
        return itseRepository.findByExpedienteId(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La etapa ITSE del expediente aún no ha sido abierta: " + expedienteId));
    }

    private ExpedienteItseDto aDto(Expediente expediente, ExpedienteItse etapa) {
        List<AnexoItseDto> anexos = Arrays.stream(TipoAnexoItse.values())
                .map(anexo -> {
                    Optional<DocumentoVersionado> documento = ultimaVersion(expediente.getId(), anexo);
                    return AnexoItseDto.builder()
                            .anexo(anexo)
                            .descripcion(anexo.getDescripcion())
                            .cargado(documento.isPresent())
                            .documentoId(documento.map(DocumentoVersionado::getId).orElse(null))
                            .version(documento.map(DocumentoVersionado::getVersion).orElse(null))
                            .nombreArchivo(documento.map(DocumentoVersionado::getNombreArchivo).orElse(null))
                            .fechaCarga(documento.map(DocumentoVersionado::getFechaCarga).orElse(null))
                            .build();
                })
                .toList();

        List<ObservacionItseDto> observaciones = observacionRepository
                .findByExpedienteItseIdOrderByFechaObservacionDesc(etapa.getId()).stream()
                .map(observacion -> ObservacionItseDto.builder()
                        .id(observacion.getId())
                        .anexo(observacion.getAnexo())
                        .tipo(observacion.getTipo())
                        .motivo(observacion.getMotivo())
                        .observadoPor(observacion.getObservadoPor())
                        .fechaObservacion(observacion.getFechaObservacion())
                        .atendida(observacion.isAtendida())
                        .build())
                .toList();

        return ExpedienteItseDto.builder()
                .expedienteId(expediente.getId())
                .numeroTramite(expediente.getNumeroTramite())
                .estado(etapa.getEstado())
                .canal(etapa.getCanal())
                .nivelDeclarado(etapa.getNivelDeclarado())
                .nivelOficial(etapa.getNivelOficial())
                .anexos(anexos)
                .observaciones(observaciones)
                .liquidacion(liquidacionDe(etapa))
                .cargadoPor(etapa.getCargadoPor())
                .fechaCarga(etapa.getFechaCarga())
                .validadoPor(etapa.getValidadoPor())
                .fechaValidacion(etapa.getFechaValidacion())
                .build();
    }
}
