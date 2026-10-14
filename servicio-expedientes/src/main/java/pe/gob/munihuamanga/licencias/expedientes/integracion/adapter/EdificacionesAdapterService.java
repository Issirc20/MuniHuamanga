package pe.gob.munihuamanga.licencias.expedientes.integracion.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.EdificacionesPort;

import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador para consulta de zonificación con Edificaciones (MuniHuamanga V2 - RF-EVA-003 / Corrección I02.1).
 * Deshabilita la emisión simulada de certificados automáticos ("CERT-ZONIF-...", "COMPATIBLE / PERMITIDO"
 * o firmas de arquitectos ficticios) que puedan interpretarse como actos administrativos oficiales.
 * La compatibilidad de zonificación debe ser dictaminada y motivada técnicamente por el evaluador municipal.
 */
@Slf4j
@Service
public class EdificacionesAdapterService implements EdificacionesPort {

    @Override
    public Map<String, Object> consultarCompatibilidadZonificacion(String direccion, String giro) {
        log.info("Edificaciones Adapter: Consulta técnica referencial para dirección '{}' con giro '{}'", direccion, giro);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("direccion", direccion);
        respuesta.put("giroSolicitado", giro);
        respuesta.put("clasificacionZonificacion", "REFERENCIAL_PDU");
        respuesta.put("compatibilidadUso", "PENDIENTE_DICTAMEN_TECNICO_EVALUADOR");
        respuesta.put("certificadoNumero", null);
        respuesta.put("simulacionOficial", false);
        respuesta.put("requiereInformeTecnico", true);
        respuesta.put("mensaje", "La compatibilidad de zonificación no puede emitirse automáticamente por simulación. Requiere dictamen técnico motivado del evaluador municipal de Edificaciones conforme al Plan de Desarrollo Urbano (PDU) vigente.");
        return respuesta;
    }
}
