package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.dto.AccionPropuestaDto;
import pe.gob.munihuamanga.licencias.common.dto.PlazoProcedimientoDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.TipoAccionPropuesta;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ServicioPlazoLegal;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Alerta a la gerencia cuando el plazo legal de evaluación vence sin resolución.
 * El sistema no emite la licencia por sí mismo: el efecto del silencio positivo se reconoce mediante acto formal.
 */
@Component
@RequiredArgsConstructor
public class ReglaSilencioPositivo implements ReglaVencimiento {

    private static final Set<EstadoExpediente> ESTADOS = EnumSet.of(
            EstadoExpediente.EN_EVALUACION, EstadoExpediente.SUBSANADO, EstadoExpediente.APTO_PARA_RESOLUCION);

    private final ServicioPlazoLegal servicioPlazoLegal;

    @Override
    public boolean aplicaA(EstadoExpediente estado) {
        return ESTADOS.contains(estado);
    }

    @Override
    public Optional<AccionPropuestaDto> evaluar(Expediente expediente, LocalDate fechaEvaluacion) {
        PlazoProcedimientoDto plazo = servicioPlazoLegal.calcular(expediente);
        if (!plazo.isVencido()) {
            return Optional.empty();
        }
        return Optional.of(AccionPropuestaDto.builder()
                .expedienteId(expediente.getId())
                .numeroTramite(expediente.getNumeroTramite())
                .estado(expediente.getEstado())
                .regla("SILENCIO_POSITIVO")
                .accion(TipoAccionPropuesta.ALERTA_SILENCIO_POSITIVO)
                .descripcion(String.format("Plazo legal de %d días hábiles vencido sin resolución.", plazo.getDiasPlazo()))
                .fechaReferencia(plazo.getFechaInicio())
                .diasTranscurridos(plazo.getDiasConsumidos())
                .diasLimite(plazo.getDiasPlazo())
                .build());
    }
}
