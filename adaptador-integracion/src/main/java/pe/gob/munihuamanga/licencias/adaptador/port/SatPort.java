package pe.gob.munihuamanga.licencias.adaptador.port;

import java.util.Map;

/**
 * Puerto de integración con el Servicio de Administración Tributaria de Huamanga (SAT).
 * Permite consultar la recaudación y conciliación de vouchers de tasa administrativa.
 */
public interface SatPort {
    Map<String, Object> validarEstadoPago(String voucherId);
}
