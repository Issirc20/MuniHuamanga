package pe.gob.munihuamanga.licencias.expedientes.integracion.port;

import java.util.Map;

/**
 * Puerto hexagonal de integración con el Servicio de Administración Tributaria de Huamanga (SAT).
 * Permite consultar la recaudación y conciliación de vouchers de tasa administrativa.
 */
public interface SatPort {
    Map<String, Object> validarEstadoPago(String voucherId);
}
