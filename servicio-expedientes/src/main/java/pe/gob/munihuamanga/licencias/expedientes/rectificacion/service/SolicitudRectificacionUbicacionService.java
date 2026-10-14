package pe.gob.munihuamanga.licencias.expedientes.rectificacion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.EvaluarRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitarRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitudRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.model.SolicitudRectificacionUbicacion;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.repository.SolicitudRectificacionUbicacionRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de solicitudes de rectificación de ubicación geográfica (Fase 6 - Criterio CA-08).
 * Impide la modificación directa por el administrado y distingue error geográfico de traslado físico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudRectificacionUbicacionService {

    private final SolicitudRectificacionUbicacionRepository solicitudRepository;
    private final EstablecimientoRepository establecimientoRepository;
    private final ExpedienteRepository expedienteRepository;

    private static final double UMBRAL_TRASLADO_FISICO_METROS = 300.0;

    @Transactional
    public SolicitudRectificacionUbicacionDto solicitarRectificacion(SolicitarRectificacionUbicacionDto dto, Authentication auth) {
        String solicitante = (auth != null) ? auth.getName() : "CIUDADANO";
        log.info("[RECTIFICACION] Solicitud de rectificación de coordenadas para establecimiento {} por {}", dto.getEstablecimientoId(), solicitante);

        Establecimiento est = establecimientoRepository.findById(dto.getEstablecimientoId())
                .orElseThrow(() -> new IllegalArgumentException("Establecimiento no encontrado: " + dto.getEstablecimientoId()));

        // Validación contextual de expediente si se especifica
        if (dto.getExpedienteId() != null) {
            Expediente exp = expedienteRepository.findById(dto.getExpedienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + dto.getExpedienteId()));
            if (exp.getEstablecimientoId() == null || !exp.getEstablecimientoId().equals(est.getId())) {
                throw new IllegalArgumentException("El expediente especificado no corresponde al establecimiento.");
            }
        }

        BigDecimal latOriginal = est.getLatitud() != null ? est.getLatitud() : BigDecimal.ZERO;
        BigDecimal lonOriginal = est.getLongitud() != null ? est.getLongitud() : BigDecimal.ZERO;

        double distancia = calcularDistanciaMetros(
                latOriginal.doubleValue(), lonOriginal.doubleValue(),
                dto.getLatitudPropuesta().doubleValue(), dto.getLongitudPropuesta().doubleValue()
        );

        // Alerta de riesgo: la distancia funciona como indicador para el evaluador humano, no como rechazo automático
        boolean alertaTraslado = distancia > UMBRAL_TRASLADO_FISICO_METROS;

        SolicitudRectificacionUbicacion solicitud = SolicitudRectificacionUbicacion.builder()
                .id(UUID.randomUUID())
                .establecimientoId(est.getId())
                .expedienteId(dto.getExpedienteId())
                .latitudOriginal(latOriginal)
                .longitudOriginal(lonOriginal)
                .latitudPropuesta(dto.getLatitudPropuesta())
                .longitudPropuesta(dto.getLongitudPropuesta())
                .distanciaMetrosAprox(distancia)
                .justificacion(dto.getJustificacion().trim())
                .documentoSustentoId(dto.getDocumentoSustentoId())
                .estado(EstadoRectificacionUbicacion.PENDIENTE)
                .solicitante(solicitante)
                .fechaSolicitud(LocalDateTime.now())
                .esTrasladoFisico(alertaTraslado)
                .build();

        SolicitudRectificacionUbicacion guardada = solicitudRepository.save(solicitud);

        // GARANTÍA TÉCNICA: Las coordenadas oficiales del establecimiento NO se modifican aquí
        log.info("[RECTIFICACION] Solicitud {} registrada como PENDIENTE. Coordenadas oficiales inalteradas.", guardada.getId());

        return mapearDto(guardada, est.getNombreComercial());
    }

    @Transactional
    public SolicitudRectificacionUbicacionDto evaluarRectificacion(UUID solicitudId, EvaluarRectificacionUbicacionDto dto, Authentication auth) {
        String evaluador = (auth != null) ? auth.getName() : "EVALUADOR";
        log.info("[RECTIFICACION] Evaluando solicitud {} por {}", solicitudId, evaluador);

        SolicitudRectificacionUbicacion sol = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de rectificación no encontrada: " + solicitudId));

        if (sol.getEstado() != EstadoRectificacionUbicacion.PENDIENTE) {
            throw new IllegalStateException("La solicitud de rectificación ya fue atendida previamente (" + sol.getEstado() + ")");
        }

        Establecimiento est = establecimientoRepository.findById(sol.getEstablecimientoId())
                .orElseThrow(() -> new IllegalArgumentException("Establecimiento no encontrado: " + sol.getEstablecimientoId()));

        sol.setEvaluador(evaluador);
        sol.setFechaEvaluacion(LocalDateTime.now());
        sol.setMotivoRespuesta(dto.getMotivoRespuesta().trim());

        // Evaluación humana motivada: la distancia es un indicador de riesgo, el evaluador califica formalmente
        if (dto.isEsTrasladoFisico()) {
            sol.setEsTrasladoFisico(true);
            sol.setEstado(EstadoRectificacionUbicacion.RECHAZADA);
            log.warn("[RECTIFICACION] Rechazada por el evaluador: La variación representa un traslado físico del local comercial. Se requiere nuevo trámite.");
        } else if (dto.isAprobado()) {
            sol.setEsTrasladoFisico(false);
            sol.setEstado(EstadoRectificacionUbicacion.APROBADA);
            // ACTUALIZACIÓN CONTROLADA: Solo tras aprobación humana motivada del funcionario competente
            est.setLatitud(sol.getLatitudPropuesta());
            est.setLongitud(sol.getLongitudPropuesta());
            establecimientoRepository.save(est);
            log.info("[RECTIFICACION] Aprobada: Coordenadas oficiales del establecimiento {} actualizadas a {}, {}",
                    est.getId(), sol.getLatitudPropuesta(), sol.getLongitudPropuesta());
        } else {
            sol.setEsTrasladoFisico(false);
            sol.setEstado(EstadoRectificacionUbicacion.RECHAZADA);
            log.info("[RECTIFICACION] Rechazada: Solicitud denegada por motivos técnicos o inconsistencias: {}", sol.getMotivoRespuesta());
        }

        SolicitudRectificacionUbicacion guardada = solicitudRepository.save(sol);
        return mapearDto(guardada, est.getNombreComercial());
    }

    @Transactional(readOnly = true)
    public List<SolicitudRectificacionUbicacionDto> listarPorEstablecimiento(UUID establecimientoId) {
        Establecimiento est = establecimientoRepository.findById(establecimientoId)
                .orElseThrow(() -> new IllegalArgumentException("Establecimiento no encontrado: " + establecimientoId));
        return solicitudRepository.findByEstablecimientoIdOrderByFechaSolicitudDesc(establecimientoId).stream()
                .map(s -> mapearDto(s, est.getNombreComercial()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudRectificacionUbicacionDto> listarPendientes() {
        return solicitudRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoRectificacionUbicacion.PENDIENTE).stream()
                .map(s -> {
                    String nombre = establecimientoRepository.findById(s.getEstablecimientoId())
                            .map(Establecimiento::getNombreComercial).orElse("Establecimiento");
                    return mapearDto(s, nombre);
                })
                .collect(Collectors.toList());
    }

    private double calcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radio de la Tierra en metros
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private SolicitudRectificacionUbicacionDto mapearDto(SolicitudRectificacionUbicacion s, String nombreEstablecimiento) {
        return SolicitudRectificacionUbicacionDto.builder()
                .id(s.getId())
                .establecimientoId(s.getEstablecimientoId())
                .establecimientoNombre(nombreEstablecimiento)
                .expedienteId(s.getExpedienteId())
                .latitudOriginal(s.getLatitudOriginal())
                .longitudOriginal(s.getLongitudOriginal())
                .latitudPropuesta(s.getLatitudPropuesta())
                .longitudPropuesta(s.getLongitudPropuesta())
                .distanciaMetrosAprox(s.getDistanciaMetrosAprox())
                .justificacion(s.getJustificacion())
                .documentoSustentoId(s.getDocumentoSustentoId())
                .estado(s.getEstado())
                .solicitante(s.getSolicitante())
                .fechaSolicitud(s.getFechaSolicitud())
                .evaluador(s.getEvaluador())
                .fechaEvaluacion(s.getFechaEvaluacion())
                .motivoRespuesta(s.getMotivoRespuesta())
                .esTrasladoFisico(s.getEsTrasladoFisico())
                .build();
    }
}
