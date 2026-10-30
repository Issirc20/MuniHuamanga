package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.TipoAccionPropuesta;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CodigoPlazo;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ParametrosPlazoService;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ServicioPlazoLegal;

/**
 * Vigila el plazo de subsanación de observaciones. Si la observación se formuló en la etapa de
 * admisibilidad, al vencer corresponde tener la solicitud por no presentada; en otro caso, el abandono.
 */
@Component
public class ReglaSubsanacionVencida extends ReglaParalizacion {

    private final ParametrosPlazoService parametros;

    public ReglaSubsanacionVencida(ServicioPlazoLegal servicioPlazoLegal, ParametrosPlazoService parametros) {
        super(servicioPlazoLegal);
        this.parametros = parametros;
    }

    @Override
    public boolean aplicaA(EstadoExpediente estado) {
        return estado == EstadoExpediente.OBSERVADO;
    }

    @Override
    protected String codigo() {
        return "SUBSANACION_VENCIDA";
    }

    @Override
    protected int plazo(Expediente expediente) {
        return esObservacionDeAdmisibilidad(expediente)
                ? parametros.dias(CodigoPlazo.SUBSANACION_ADMISIBILIDAD)
                : parametros.dias(CodigoPlazo.SUBSANACION_EVALUACION);
    }

    @Override
    protected TipoAccionPropuesta accionAlVencer(Expediente expediente) {
        return esObservacionDeAdmisibilidad(expediente)
                ? TipoAccionPropuesta.DECLARAR_SOLICITUD_NO_PRESENTADA
                : TipoAccionPropuesta.DECLARAR_ABANDONO;
    }

    @Override
    protected String descripcionEtapa(Expediente expediente) {
        return esObservacionDeAdmisibilidad(expediente)
                ? "Subsanación de observaciones de admisibilidad"
                : "Subsanación de observaciones de evaluación";
    }

    private static boolean esObservacionDeAdmisibilidad(Expediente expediente) {
        return expediente.getEstadoPrevioObservacion() == EstadoExpediente.PRESENTADO;
    }
}
