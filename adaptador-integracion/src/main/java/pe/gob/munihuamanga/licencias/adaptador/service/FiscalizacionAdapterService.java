package pe.gob.munihuamanga.licencias.adaptador.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.adaptador.port.FiscalizacionPort;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador Stub para la Unidad de Fiscalización y Control Municipal (Fase 2).
 * Simula el registro y consulta de actas de fiscalización posterior en establecimientos.
 */
@Slf4j
@Service
public class FiscalizacionAdapterService implements FiscalizacionPort {

    @Override
    public Map<String, Object> registrarActaFiscalizacion(String codigoLicencia, String resultado, String inspector) {
        log.info("Fiscalización Adapter: Registrando acta de inspección para licencia {} con resultado {}", codigoLicencia, resultado);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("codigoLicencia", codigoLicencia);
        respuesta.put("actaNumero", "ACTA-FISC-2026-" + (int)(Math.random() * 90000 + 10000));
        respuesta.put("fechaInspeccion", LocalDateTime.now());
        respuesta.put("resultadoFiscalizacion", resultado != null ? resultado : "CONFORME");
        respuesta.put("inspectorMunicipal", inspector != null ? inspector : "Abog. Marco Solís (Fiscalizador)");
        respuesta.put("observaciones", "El establecimiento comercial mantiene el giro y condiciones autorizadas en la licencia original.");
        return respuesta;
    }
}
