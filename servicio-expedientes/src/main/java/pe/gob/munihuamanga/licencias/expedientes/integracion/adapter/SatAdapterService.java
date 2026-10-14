package pe.gob.munihuamanga.licencias.expedientes.integracion.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.SatPort;

import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador para consulta de estado tributario SAT Huamanga (MuniHuamanga V2 - ERS-1.3 / Corrección I02.1).
 * Deshabilita la simulación de pagos automáticos ("PAGADO" / números falsos de operación)
 * para evitar que sean interpretados como validaciones oficiales.
 * La conciliación de pagos se realiza mediante registro manual en ventanilla/caja por el CAJERO.
 */
@Slf4j
@Service
public class SatAdapterService implements SatPort {

    @Override
    public Map<String, Object> validarEstadoPago(String voucherId) {
        log.info("SAT Adapter: Consulta referencial de conciliación para voucher {}", voucherId);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("voucherId", voucherId);
        respuesta.put("estadoPago", "REQUIERE_VALIDACION_MANUAL_VENTANILLA");
        respuesta.put("valido", false);
        respuesta.put("simulacionOficial", false);
        respuesta.put("requiereValidacionManual", true);
        respuesta.put("mensaje", "La interoperabilidad automática con SAT Huamanga se encuentra en proceso de homologación técnica (I08). La validación y conciliación de vouchers debe ser realizada manualmente por el cajero municipal.");
        respuesta.put("canalOficial", "Caja Central de la Municipalidad Provincial de Huamanga / Ventanilla SAT");
        return respuesta;
    }
}
