package pe.gob.munihuamanga.licencias.adaptador.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.adaptador.port.EdificacionesPort;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador Stub para la Subgerencia de Edificaciones y Urbanismo (Fase 2).
 * Simula la consulta de zonificación y compatibilidad de uso del Plan de Desarrollo Urbano.
 */
@Slf4j
@Service
public class EdificacionesAdapterService implements EdificacionesPort {

    @Override
    public Map<String, Object> consultarCompatibilidadZonificacion(String direccion, String giro) {
        log.info("Edificaciones Adapter: Evaluando zonificación para dirección '{}' con giro '{}'", direccion, giro);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("direccion", direccion);
        respuesta.put("giroSolicitado", giro);
        respuesta.put("clasificacionZonificacion", "ZRE-CH (Zona de Reglamentación Especial - Centro Histórico de Huamanga)");
        respuesta.put("compatibilidadUso", "COMPATIBLE / PERMITIDO");
        respuesta.put("certificadoNumero", "CERT-ZONIF-2026-" + (int)(Math.random() * 9000 + 1000));
        respuesta.put("fechaEmision", LocalDateTime.now());
        respuesta.put("evaluadorTecnico", "Arq. Lucía Quispe Morales (CAP 1245)");
        respuesta.put("vigencia", "VÁLIDO PARA PROCEDIMIENTO TUPA");
        return respuesta;
    }
}
