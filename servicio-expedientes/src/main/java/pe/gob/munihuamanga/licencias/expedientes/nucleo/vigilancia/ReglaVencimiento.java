package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import pe.gob.munihuamanga.licencias.common.dto.AccionPropuestaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Regla de vigilancia de plazos aplicable a los expedientes en determinado estado.
 * Las reglas solo proponen acciones; las decisiones discrecionales las adopta el funcionario competente.
 */
public interface ReglaVencimiento {

    boolean aplicaA(EstadoExpediente estado);

    Optional<AccionPropuestaDto> evaluar(Expediente expediente, LocalDate fechaEvaluacion);
}
