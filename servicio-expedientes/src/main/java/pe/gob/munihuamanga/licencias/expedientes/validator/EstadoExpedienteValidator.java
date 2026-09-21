package pe.gob.munihuamanga.licencias.expedientes.validator;

import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.exception.TransicionInvalidaException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Componente que valida las transiciones válidas de la máquina de estados del expediente
 * según el flujo normativo de la Ley N° 28976 y el modelo de arquitectura C4.
 */
@Component
public class EstadoExpedienteValidator {

    private static final Map<EstadoExpediente, Set<EstadoExpediente>> TRANSICIONES_PERMITIDAS;

    static {
        Map<EstadoExpediente, Set<EstadoExpediente>> map = new EnumMap<>(EstadoExpediente.class);

        // Desde FORMATOS_GENERADOS se pasa a DOCUMENTOS_VALIDADOS (o RECHAZADO si se desestima)
        map.put(EstadoExpediente.FORMATOS_GENERADOS, EnumSet.of(
                EstadoExpediente.DOCUMENTOS_VALIDADOS,
                EstadoExpediente.RECHAZADO
        ));

        // Desde DOCUMENTOS_VALIDADOS se pasa a EN_EVALUACION_FINAL (o RECHAZADO)
        map.put(EstadoExpediente.DOCUMENTOS_VALIDADOS, EnumSet.of(
                EstadoExpediente.EN_EVALUACION_FINAL,
                EstadoExpediente.RECHAZADO
        ));

        // Desde EN_EVALUACION_FINAL se resuelve como APROBADO o RECHAZADO
        map.put(EstadoExpediente.EN_EVALUACION_FINAL, EnumSet.of(
                EstadoExpediente.APROBADO,
                EstadoExpediente.RECHAZADO
        ));

        // Estados terminales: no admiten nuevas transiciones
        map.put(EstadoExpediente.APROBADO, Collections.emptySet());
        map.put(EstadoExpediente.RECHAZADO, Collections.emptySet());

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
}
