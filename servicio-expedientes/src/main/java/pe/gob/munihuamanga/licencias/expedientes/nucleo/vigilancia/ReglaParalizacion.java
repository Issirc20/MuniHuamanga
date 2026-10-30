package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import pe.gob.munihuamanga.licencias.common.dto.AccionPropuestaDto;
import pe.gob.munihuamanga.licencias.common.enums.TipoAccionPropuesta;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ServicioPlazoLegal;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Base de las reglas que vigilan la paralización de una etapa a cargo del administrado.
 * Propone un aviso al administrado al alcanzar el 60 % del plazo y la acción de cierre al vencer.
 */
abstract class ReglaParalizacion implements ReglaVencimiento {

    private static final double UMBRAL_AVISO = 0.6;

    private final ServicioPlazoLegal servicioPlazoLegal;

    protected ReglaParalizacion(ServicioPlazoLegal servicioPlazoLegal) {
        this.servicioPlazoLegal = servicioPlazoLegal;
    }

    protected abstract String codigo();

    protected abstract int plazo(Expediente expediente);

    protected abstract TipoAccionPropuesta accionAlVencer(Expediente expediente);

    protected abstract String descripcionEtapa(Expediente expediente);

    @Override
    public Optional<AccionPropuestaDto> evaluar(Expediente expediente, LocalDate fechaEvaluacion) {
        Optional<LocalDate> ingreso = servicioPlazoLegal.fechaIngresoEstadoActual(expediente);
        if (ingreso.isEmpty()) {
            return Optional.empty();
        }
        int transcurridos = servicioPlazoLegal.diasHabilesDesde(ingreso.get());
        int limite = plazo(expediente);

        TipoAccionPropuesta accion;
        String descripcion;
        if (transcurridos >= limite) {
            accion = accionAlVencer(expediente);
            descripcion = String.format("%s: venció el plazo de %d días hábiles.", descripcionEtapa(expediente), limite);
        } else if (transcurridos >= Math.ceil(limite * UMBRAL_AVISO)) {
            accion = TipoAccionPropuesta.AVISO_ADMINISTRADO;
            descripcion = String.format("%s: transcurrieron %d de %d días hábiles.", descripcionEtapa(expediente), transcurridos, limite);
        } else {
            return Optional.empty();
        }

        return Optional.of(AccionPropuestaDto.builder()
                .expedienteId(expediente.getId())
                .numeroTramite(expediente.getNumeroTramite())
                .estado(expediente.getEstado())
                .regla(codigo())
                .accion(accion)
                .descripcion(descripcion)
                .fechaReferencia(ingreso.get())
                .diasTranscurridos(transcurridos)
                .diasLimite(limite)
                .build());
    }
}
