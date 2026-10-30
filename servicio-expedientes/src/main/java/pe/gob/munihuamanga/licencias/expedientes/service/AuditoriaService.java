package pe.gob.munihuamanga.licencias.expedientes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.HistorialEstado;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.TransicionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.HistorialRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Registro inmutable de la trazabilidad del expediente (historial de estados y anotaciones).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int LONGITUD_MAXIMA_MOTIVO = 500;

    private final HistorialRepository historialRepository;

    /**
     * Registra una transición de estado aplicada por el agregado Expediente.
     */
    @Transactional
    public void registrarTransicion(TransicionExpediente transicion) {
        HistorialEstado historial = HistorialEstado.builder()
                .id(UUID.randomUUID())
                .expedienteId(transicion.expedienteId())
                .estadoAnterior(transicion.estadoAnterior())
                .estadoNuevo(transicion.estadoNuevo())
                .evento(transicion.evento().name())
                .usuarioId(transicion.actor().usuarioId())
                .usuario(transicion.actor().nombreParaRegistro())
                .motivo(recortar(transicion.sustento(), "Transición registrada sin sustento adicional"))
                .fecha(transicion.momento())
                .build();
        historialRepository.save(historial);
        log.info("[TRAZABILIDAD] Expediente {}: {} -> {} por evento {}",
                transicion.numeroTramite(), transicion.estadoAnterior(), transicion.estadoNuevo(), transicion.evento());
    }

    /**
     * Registra la apertura del expediente o una anotación que no modifica su estado.
     */
    @Transactional
    public void registrarTransicion(UUID expedienteId, EstadoExpediente estadoAnterior, EstadoExpediente estadoNuevo, String usuario, String motivo) {
        HistorialEstado historial = HistorialEstado.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedienteId)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .usuario(usuario != null && !usuario.isBlank() ? usuario : "SISTEMA")
                .motivo(recortar(motivo, "Anotación del procedimiento"))
                .fecha(LocalDateTime.now())
                .build();

        historialRepository.save(historial);
        log.info("[TRAZABILIDAD] Anotación registrada para expediente {}: {} -> {}", expedienteId, estadoAnterior, estadoNuevo);
    }

    @Transactional(readOnly = true)
    public List<HistorialEstado> obtenerHistorial(UUID expedienteId) {
        return historialRepository.findByExpedienteIdOrderByFechaAsc(expedienteId);
    }

    private static String recortar(String texto, String porDefecto) {
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        String limpio = texto.trim();
        return limpio.length() <= LONGITUD_MAXIMA_MOTIVO ? limpio : limpio.substring(0, LONGITUD_MAXIMA_MOTIVO);
    }
}
