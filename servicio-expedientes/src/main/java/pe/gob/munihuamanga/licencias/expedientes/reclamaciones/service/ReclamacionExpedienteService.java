package pe.gob.munihuamanga.licencias.expedientes.reclamaciones.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ReclamacionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ReclamarExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolverReclamacionDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoReclamacion;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.reclamaciones.model.ReclamacionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.reclamaciones.repository.ReclamacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReclamacionExpedienteService {

    private final ReclamacionExpedienteRepository reclamacionRepository;
    private final ExpedienteRepository expedienteRepository;
    private final CuentaRepository cuentaRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public ReclamacionExpedienteDto solicitarReclamacion(UUID expedienteId, ReclamarExpedienteDto dto, String username) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente no encontrado con ID: " + expedienteId));

        Cuenta cuenta = cuentaRepository.findById(dto.getCuentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con ID: " + dto.getCuentaId()));

        if (expediente.getCuentaId() != null && expediente.getCuentaId().equals(cuenta.getId())) {
            throw new IllegalStateException("El expediente ya se encuentra formalmente vinculado a su cuenta.");
        }

        if (reclamacionRepository.existsByExpedienteIdAndEstado(expedienteId, EstadoReclamacion.PENDIENTE)) {
            throw new IllegalStateException("Ya existe una solicitud de reclamación pendiente para este expediente.");
        }

        ReclamacionExpediente reclamacion = ReclamacionExpediente.builder()
                .id(UUID.randomUUID())
                .expediente(expediente)
                .cuenta(cuenta)
                .solicitanteUsername(username)
                .estado(EstadoReclamacion.PENDIENTE)
                .evidenciaTitularidad(dto.getEvidenciaTitularidad().trim())
                .motivo(dto.getMotivo() != null ? dto.getMotivo().trim() : "Reclamación de titularidad de expediente histórico")
                .fechaSolicitud(LocalDateTime.now())
                .build();

        ReclamacionExpediente guardada = reclamacionRepository.save(reclamacion);
        log.info("[RECLAMACION-TITULARIDAD] Nueva solicitud ID: {} para expediente: {} desde cuenta: {}",
                guardada.getId(), expediente.getNumeroTramite(), cuenta.getId());

        return mapearADto(guardada);
    }

    @Transactional
    public ReclamacionExpedienteDto resolverReclamacion(
            UUID reclamacionId,
            ResolverReclamacionDto dto,
            UUID evaluadorId,
            String evaluadorUsername
    ) {
        ReclamacionExpediente reclamacion = reclamacionRepository.findById(reclamacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reclamación no encontrada con ID: " + reclamacionId));

        if (reclamacion.getEstado() != EstadoReclamacion.PENDIENTE) {
            throw new IllegalStateException("La reclamación ya fue resuelta con estado: " + reclamacion.getEstado());
        }

        LocalDateTime ahora = LocalDateTime.now();
        reclamacion.setFechaResolucion(ahora);
        reclamacion.setEvaluadorId(evaluadorId);
        reclamacion.setMotivoResolucion(dto.getMotivoResolucion().trim());

        if (Boolean.TRUE.equals(dto.getAprobado())) {
            reclamacion.setEstado(EstadoReclamacion.APROBADA);

            // Vinculación formal y verificada de titularidad
            Expediente exp = reclamacion.getExpediente();
            UUID cuentaId = reclamacion.getCuenta().getId();
            exp.setCuentaId(cuentaId);
            exp.setSolicitanteId(cuentaId);
            expedienteRepository.save(exp);

            auditoriaService.registrarTransicion(
                    exp.getId(),
                    exp.getEstado(),
                    exp.getEstado(),
                    evaluadorUsername,
                    "Titularidad histórica verificada y vinculada a cuenta " + cuentaId + ". Motivo: " + dto.getMotivoResolucion()
            );

            log.info("[RECLAMACION-APROBADA] Titularidad de expediente {} vinculada exitosamente a cuenta {}",
                    exp.getNumeroTramite(), cuentaId);
        } else {
            reclamacion.setEstado(EstadoReclamacion.RECHAZADA);
            log.info("[RECLAMACION-RECHAZADA] Reclamación {} rechazada por funcionario {}. Motivo: {}",
                    reclamacionId, evaluadorUsername, dto.getMotivoResolucion());
        }

        ReclamacionExpediente resuelta = reclamacionRepository.save(reclamacion);
        return mapearADto(resuelta);
    }

    @Transactional(readOnly = true)
    public List<ReclamacionExpedienteDto> listarPorCuenta(UUID cuentaId) {
        return reclamacionRepository.findByCuentaId(cuentaId).stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReclamacionExpedienteDto> listarPendientes() {
        return reclamacionRepository.findByEstado(EstadoReclamacion.PENDIENTE).stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    private ReclamacionExpedienteDto mapearADto(ReclamacionExpediente r) {
        return ReclamacionExpedienteDto.builder()
                .id(r.getId())
                .expedienteId(r.getExpediente().getId())
                .numeroTramite(r.getExpediente().getNumeroTramite())
                .cuentaId(r.getCuenta().getId())
                .titularNombre(r.getCuenta().getRazonSocialONombre())
                .solicitanteUsername(r.getSolicitanteUsername())
                .estado(r.getEstado())
                .evidenciaTitularidad(r.getEvidenciaTitularidad())
                .motivo(r.getMotivo())
                .fechaSolicitud(r.getFechaSolicitud())
                .fechaResolucion(r.getFechaResolucion())
                .evaluadorId(r.getEvaluadorId())
                .motivoResolucion(r.getMotivoResolucion())
                .build();
    }
}
