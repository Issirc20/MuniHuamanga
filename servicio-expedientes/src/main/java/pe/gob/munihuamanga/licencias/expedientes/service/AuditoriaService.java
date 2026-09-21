package pe.gob.munihuamanga.licencias.expedientes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.HistorialEstado;
import pe.gob.munihuamanga.licencias.expedientes.repository.HistorialRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de auditoría para registro y trazabilidad de los cambios de estado del expediente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final HistorialRepository historialRepository;

    @Transactional
    public void registrarTransicion(UUID expedienteId, EstadoExpediente estadoAnterior, EstadoExpediente estadoNuevo, String usuario, String motivo) {
        HistorialEstado historial = HistorialEstado.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedienteId)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .usuario(usuario != null && !usuario.isBlank() ? usuario : "SISTEMA")
                .motivo(motivo != null && !motivo.isBlank() ? motivo : "Transición automática de flujo")
                .fecha(LocalDateTime.now())
                .build();

        historialRepository.save(historial);
        log.info("Auditoría registrada: Expediente={}, {} -> {}, Usuario={}", expedienteId, estadoAnterior, estadoNuevo, usuario);
    }

    @Transactional(readOnly = true)
    public List<HistorialEstado> obtenerHistorial(UUID expedienteId) {
        return historialRepository.findByExpedienteIdOrderByFechaAsc(expedienteId);
    }
}
