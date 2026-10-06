package pe.gob.munihuamanga.licencias.expedientes.integracion.port;

import java.util.Map;

/**
 * Puerto hexagonal de integración con la Unidad de Fiscalización y Control Municipal.
 * Permite registrar y consultar actas de fiscalización posterior en comercios autorizados.
 */
public interface FiscalizacionPort {
    Map<String, Object> registrarActaFiscalizacion(String codigoLicencia, String resultado, String inspector);
}
