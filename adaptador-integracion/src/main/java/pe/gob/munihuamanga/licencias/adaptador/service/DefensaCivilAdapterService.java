package pe.gob.munihuamanga.licencias.adaptador.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.adaptador.port.DefensaCivilPort;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador Stub para Defensa Civil / Oficina de Gestión de Riesgo (Fase 2).
 * Simula el resultado de la Inspección Técnica de Seguridad en Edificaciones (ITSE).
 */
@Slf4j
@Service
public class DefensaCivilAdapterService implements DefensaCivilPort {

    @Override
    public Map<String, Object> simularDictamenItse(String numeroTramite, NivelRiesgo nivelRiesgo) {
        log.info("Defensa Civil Adapter: Generando dictamen ITSE para {} con riesgo {}", numeroTramite, nivelRiesgo);

        String modalidad = (nivelRiesgo == NivelRiesgo.BAJO || nivelRiesgo == NivelRiesgo.MEDIO)
                ? "ITSE POSTERIOR (Inspección post-licencia)"
                : "ITSE PREVIA (Inspección previa obligatoria)";

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("numeroTramite", numeroTramite);
        respuesta.put("nivelRiesgo", nivelRiesgo);
        respuesta.put("modalidadItse", modalidad);
        respuesta.put("informeTecnicoNumero", "INF-ITSE-2026-" + (int)(Math.random() * 9000 + 1000));
        respuesta.put("resultadoInspeccion", "FAVORABLE");
        respuesta.put("inspectorResponsable", "Ing. Roberto Palomino Prado (CIP 78451)");
        respuesta.put("fechaDictamen", LocalDateTime.now());
        respuesta.put("cumpleCondicionesSeguridad", true);
        return respuesta;
    }
}
