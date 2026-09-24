package pe.gob.munihuamanga.licencias.adaptador.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.adaptador.port.SatPort;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador Stub para el SAT Huamanga (Fase 2).
 * Simula la respuesta del sistema tributario municipal para verificación y conciliación de vouchers.
 */
@Slf4j
@Service
public class SatAdapterService implements SatPort {

    @Override
    public Map<String, Object> validarEstadoPago(String voucherId) {
        log.info("SAT Adapter: Validando conciliación bancaria para voucher {}", voucherId);

        boolean esValido = voucherId != null && voucherId.startsWith("VCH-");

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("voucherId", voucherId);
        respuesta.put("estadoPago", esValido ? "PAGADO" : "PENDIENTE");
        respuesta.put("fechaPago", esValido ? LocalDateTime.now().minusHours(1) : null);
        respuesta.put("montoRecaudado", esValido ? 218.00 : 0.00);
        respuesta.put("entidadRecaudadora", "Servicio de Administración Tributaria de Huamanga (SAT)");
        respuesta.put("canalPago", "Ventanilla Central / Red Bancaria Autorizada");
        respuesta.put("numeroOperacionBancaria", "OP-SAT-" + (int)(Math.random() * 900000 + 100000));
        return respuesta;
    }
}
