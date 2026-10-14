package pe.gob.munihuamanga.licencias.expedientes.licencias.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de control de abuso y tasa de peticiones (Rate Limiter) en memoria
 * para endpoints públicos de consulta QR (M10 - Incremento I09).
 * Previene ataques de denegación de servicio (DoS) y enumeración por fuerza bruta.
 */
@Slf4j
@Service
public class RateLimiterService {

    private static final int MAX_SOLICITUDES_POR_VENTANA = 60;
    private static final long VENTANA_TIEMPO_MS = 60_000L; // 1 minuto

    private final Map<String, Deque<Long>> historialPorIp = new ConcurrentHashMap<>();

    /**
     * Verifica si una solicitud proveniente de clientIp está dentro del límite permitido.
     * @param clientIp Dirección IP del cliente
     * @return true si la solicitud es admitida, false si ha superado el umbral.
     */
    public boolean admitirSolicitud(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "UNKNOWN";
        }

        long ahora = System.currentTimeMillis();
        Deque<Long> timestamps = historialPorIp.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Eliminar registros fuera de la ventana móvil
            while (!timestamps.isEmpty() && ahora - timestamps.peekFirst() > VENTANA_TIEMPO_MS) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_SOLICITUDES_POR_VENTANA) {
                log.warn("[RATE-LIMIT] IP {} ha excedido el límite de {} consultas por minuto.", clientIp, MAX_SOLICITUDES_POR_VENTANA);
                return false;
            }

            timestamps.addLast(ahora);
            return true;
        }
    }

    /**
     * Reinicia el historial para una IP (útil para pruebas automatizadas).
     */
    public void reiniciarParaIp(String clientIp) {
        if (clientIp != null) {
            historialPorIp.remove(clientIp);
        }
    }

    /**
     * Limpia todo el historial del rate limiter.
     */
    public void reiniciarTodo() {
        historialPorIp.clear();
    }
}
