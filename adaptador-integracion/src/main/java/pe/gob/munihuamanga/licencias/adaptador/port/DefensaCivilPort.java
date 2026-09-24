package pe.gob.munihuamanga.licencias.adaptador.port;

import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.util.Map;

/**
 * Puerto de integración con la Subgerencia de Defensa Civil (Oficina de Gestión de Riesgo de Desastres).
 * Regula la Inspección Técnica de Seguridad en Edificaciones (ITSE) según D.S. N° 002-2018-PCM.
 */
public interface DefensaCivilPort {
    Map<String, Object> simularDictamenItse(String numeroTramite, NivelRiesgo nivelRiesgo);
}
