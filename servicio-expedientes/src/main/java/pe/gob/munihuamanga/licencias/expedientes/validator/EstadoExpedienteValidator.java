package pe.gob.munihuamanga.licencias.expedientes.validator;

import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.exception.TransicionInvalidaException;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Componente que valida las transiciones válidas de la máquina de estados del expediente
 * según el flujo normativo de la Ley N° 28976, D.S. 002-2018-PCM y el modelo de arquitectura C4.
 */
@Component
public class EstadoExpedienteValidator {

    private static final Map<EstadoExpediente, Set<EstadoExpediente>> TRANSICIONES_PERMITIDAS;

    static {
        Map<EstadoExpediente, Set<EstadoExpediente>> map = new EnumMap<>(EstadoExpediente.class);

        // Desde BORRADOR: formalización ciudadana hacia PRESENTADO
        map.put(EstadoExpediente.BORRADOR, EnumSet.of(
                EstadoExpediente.PRESENTADO
        ));

        // Desde PRESENTADO se pasa a calificación técnica, evaluación o rechazo/observación
        map.put(EstadoExpediente.PRESENTADO, EnumSet.of(
                EstadoExpediente.DOCUMENTOS_VALIDADOS,
                EstadoExpediente.EN_EVALUACION,
                EstadoExpediente.EN_EVALUACION_FINAL,
                EstadoExpediente.OBSERVADO,
                EstadoExpediente.RECHAZADO,
                EstadoExpediente.DENEGADO
        ));

        // Desde FORMATOS_GENERADOS se pasa a DOCUMENTOS_VALIDADOS, OBSERVADO o RECHAZADO
        map.put(EstadoExpediente.FORMATOS_GENERADOS, EnumSet.of(
                EstadoExpediente.DOCUMENTOS_VALIDADOS,
                EstadoExpediente.EN_EVALUACION,
                EstadoExpediente.OBSERVADO,
                EstadoExpediente.RECHAZADO,
                EstadoExpediente.DENEGADO
        ));

        // Desde DOCUMENTOS_VALIDADOS se pasa a evaluación técnica, habilitación resolutiva u observaciones
        map.put(EstadoExpediente.DOCUMENTOS_VALIDADOS, EnumSet.of(
                EstadoExpediente.EN_EVALUACION,
                EstadoExpediente.EN_EVALUACION_FINAL,
                EstadoExpediente.APTO_PARA_RESOLUCION,
                EstadoExpediente.OBSERVADO,
                EstadoExpediente.RECHAZADO,
                EstadoExpediente.DENEGADO
        ));

        // Desde EN_EVALUACION se pasa a APTO_PARA_RESOLUCION, APROBADO, OBSERVADO, RECHAZADO o DENEGADO
        map.put(EstadoExpediente.EN_EVALUACION, EnumSet.of(
                EstadoExpediente.APTO_PARA_RESOLUCION,
                EstadoExpediente.APROBADO,
                EstadoExpediente.OBSERVADO,
                EstadoExpediente.RECHAZADO,
                EstadoExpediente.DENEGADO
        ));

        // Desde EN_EVALUACION_FINAL se pasa a APTO_PARA_RESOLUCION, APROBADO, OBSERVADO o RECHAZADO
        map.put(EstadoExpediente.EN_EVALUACION_FINAL, EnumSet.of(
                EstadoExpediente.APTO_PARA_RESOLUCION,
                EstadoExpediente.APROBADO,
                EstadoExpediente.OBSERVADO,
                EstadoExpediente.RECHAZADO,
                EstadoExpediente.DENEGADO
        ));

        // Desde APTO_PARA_RESOLUCION: informe favorable aprobado, habilitado para resolución (I08)
        map.put(EstadoExpediente.APTO_PARA_RESOLUCION, EnumSet.of(
                EstadoExpediente.APROBADO,
                EstadoExpediente.OBSERVADO,
                EstadoExpediente.RECHAZADO,
                EstadoExpediente.DENEGADO
        ));

        // Desde OBSERVADO (Ley N° 27444 LPAG): el administrado subsana retornando al flujo o se desestima
        map.put(EstadoExpediente.OBSERVADO, EnumSet.of(
                EstadoExpediente.SUBSANADO,
                EstadoExpediente.PRESENTADO,
                EstadoExpediente.FORMATOS_GENERADOS,
                EstadoExpediente.DOCUMENTOS_VALIDADOS,
                EstadoExpediente.EN_EVALUACION,
                EstadoExpediente.EN_EVALUACION_FINAL,
                EstadoExpediente.APTO_PARA_RESOLUCION,
                EstadoExpediente.RECHAZADO,
                EstadoExpediente.DENEGADO
        ));

        // Desde SUBSANADO (Art. 136 LPAG): reingreso a evaluación técnica
        map.put(EstadoExpediente.SUBSANADO, EnumSet.of(
                EstadoExpediente.DOCUMENTOS_VALIDADOS,
                EstadoExpediente.EN_EVALUACION,
                EstadoExpediente.EN_EVALUACION_FINAL,
                EstadoExpediente.APTO_PARA_RESOLUCION,
                EstadoExpediente.OBSERVADO,
                EstadoExpediente.RECHAZADO,
                EstadoExpediente.DENEGADO
        ));

        // Desde APROBADO: dictamen favorable habilita la emisión oficial de licencia firmada digitalmente
        map.put(EstadoExpediente.APROBADO, EnumSet.of(
                EstadoExpediente.EMITIDO
        ));

        // Desde EMITIDO: notificación y cierre administrativo del procedimiento
        map.put(EstadoExpediente.EMITIDO, EnumSet.of(
                EstadoExpediente.CONCLUIDO
        ));

        // Estados terminales: no admiten nuevas transiciones
        map.put(EstadoExpediente.RECHAZADO, Collections.emptySet());
        map.put(EstadoExpediente.DENEGADO, Collections.emptySet());
        map.put(EstadoExpediente.CONCLUIDO, Collections.emptySet());

        TRANSICIONES_PERMITIDAS = Collections.unmodifiableMap(map);
    }

    /**
     * Valida que la transición entre el estado actual y el nuevo estado esté permitida.
     *
     * @param actual Estado actual del expediente.
     * @param nuevo  Nuevo estado propuesto.
     * @throws TransicionInvalidaException si la transición no es válida.
     */
    public void validarTransicion(EstadoExpediente actual, EstadoExpediente nuevo) {
        if (actual == null || nuevo == null) {
            throw new TransicionInvalidaException("El estado actual y el nuevo estado no pueden ser nulos.");
        }

        if (actual == nuevo) {
            throw new TransicionInvalidaException(String.format("El expediente ya se encuentra en el estado '%s'.", actual));
        }

        Set<EstadoExpediente> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(actual, Collections.emptySet());
        if (!permitidos.contains(nuevo)) {
            throw new TransicionInvalidaException(actual, nuevo);
        }
    }

    /**
     * Valida los requisitos de fondo exigidos por la Ley N° 28976 antes de emitir resolución favorable.
     */
    public void validarAprobacion(Expediente expediente) {
        if (expediente == null) {
            throw new IllegalArgumentException("El expediente no puede ser nulo.");
        }

        validarTransicion(expediente.getEstado(), EstadoExpediente.APROBADO);

        if (expediente.getNivelRiesgo() == null) {
            throw new TransicionInvalidaException("No se puede aprobar: falta registrar la clasificación de riesgo ITSE por parte de Defensa Civil.");
        }

        if (expediente.getVoucherId() == null) {
            throw new TransicionInvalidaException("No se puede aprobar: el administrado aún no ha cancelado la tasa administrativa ante el SAT.");
        }
    }
}
