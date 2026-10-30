package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.TipoAccionPropuesta;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CodigoPlazo;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ParametrosPlazoService;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ServicioPlazoLegal;

/**
 * Vigila la espera del pago de la tasa liquidada.
 */
@Component
public class ReglaAbandonoPago extends ReglaParalizacion {

    private final ParametrosPlazoService parametros;

    public ReglaAbandonoPago(ServicioPlazoLegal servicioPlazoLegal, ParametrosPlazoService parametros) {
        super(servicioPlazoLegal);
        this.parametros = parametros;
    }

    @Override
    public boolean aplicaA(EstadoExpediente estado) {
        return estado == EstadoExpediente.PENDIENTE_PAGO;
    }

    @Override
    protected String codigo() {
        return "ABANDONO_PAGO";
    }

    @Override
    protected int plazo(Expediente expediente) {
        return parametros.dias(CodigoPlazo.ABANDONO_PAGO);
    }

    @Override
    protected TipoAccionPropuesta accionAlVencer(Expediente expediente) {
        return TipoAccionPropuesta.DECLARAR_ABANDONO;
    }

    @Override
    protected String descripcionEtapa(Expediente expediente) {
        return "Pago de la tasa liquidada";
    }
}
