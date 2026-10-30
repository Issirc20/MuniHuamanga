package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.TipoAccionPropuesta;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CodigoPlazo;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ParametrosPlazoService;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ServicioPlazoLegal;

/**
 * Vigila la espera de los anexos ITSE firmados por Defensa Civil.
 */
@Component
public class ReglaAbandonoItse extends ReglaParalizacion {

    private final ParametrosPlazoService parametros;

    public ReglaAbandonoItse(ServicioPlazoLegal servicioPlazoLegal, ParametrosPlazoService parametros) {
        super(servicioPlazoLegal);
        this.parametros = parametros;
    }

    @Override
    public boolean aplicaA(EstadoExpediente estado) {
        return estado == EstadoExpediente.ITSE_EN_TRAMITE;
    }

    @Override
    protected String codigo() {
        return "ABANDONO_ITSE";
    }

    @Override
    protected int plazo(Expediente expediente) {
        return parametros.dias(CodigoPlazo.ABANDONO_ITSE);
    }

    @Override
    protected TipoAccionPropuesta accionAlVencer(Expediente expediente) {
        return TipoAccionPropuesta.DECLARAR_ABANDONO;
    }

    @Override
    protected String descripcionEtapa(Expediente expediente) {
        return "Carga de anexos ITSE firmados";
    }
}
