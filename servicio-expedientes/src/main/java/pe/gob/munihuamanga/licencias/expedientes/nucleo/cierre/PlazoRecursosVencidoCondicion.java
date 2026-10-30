package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CalculadoraPlazos;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CodigoPlazo;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ParametrosPlazoService;

import java.time.LocalDate;

/**
 * Debe haber vencido el plazo para interponer recursos administrativos desde la notificación.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class PlazoRecursosVencidoCondicion implements CondicionCierre {

    static final String CODIGO = "PLAZO_RECURSOS_VENCIDO";

    private final LicenciaEmitidaRepository licenciaRepository;
    private final CalculadoraPlazos calculadora;
    private final ParametrosPlazoService parametros;

    @Override
    public ResultadoCondicion evaluar(Expediente expediente, LocalDate fechaEvaluacion) {
        return licenciaRepository.findByExpedienteId(expediente.getId())
                .filter(licencia -> licencia.estaNotificada())
                .map(licencia -> {
                    LocalDate vencimiento = calculadora.sumarDiasHabiles(
                            licencia.getFechaNotificacion().toLocalDate(),
                            parametros.dias(CodigoPlazo.RECURSOS_ADMINISTRATIVOS));
                    return fechaEvaluacion.isAfter(vencimiento)
                            ? ResultadoCondicion.cumplida(CODIGO, "Plazo de recursos vencido el " + vencimiento + ".")
                            : ResultadoCondicion.pendiente(CODIGO, "El plazo de recursos vence el " + vencimiento + ".");
                })
                .orElse(ResultadoCondicion.pendiente(CODIGO, "El plazo de recursos no ha iniciado: falta la notificación."));
    }
}
