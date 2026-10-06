package pe.gob.munihuamanga.licencias.expedientes.integracion.port;

import java.util.Map;

/**
 * Puerto hexagonal de integración con la Gerencia de Desarrollo Urbano / Subgerencia de Edificaciones.
 * Permite consultar la zonificación y compatibilidad de uso del Plan de Desarrollo Urbano (PDU).
 */
public interface EdificacionesPort {
    Map<String, Object> consultarCompatibilidadZonificacion(String direccion, String giro);
}
