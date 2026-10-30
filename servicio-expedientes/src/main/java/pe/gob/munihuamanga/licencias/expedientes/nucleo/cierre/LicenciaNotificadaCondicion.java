package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.LocalDate;

/**
 * La licencia debe haber sido notificada al administrado.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class LicenciaNotificadaCondicion implements CondicionCierre {

    static final String CODIGO = "LICENCIA_NOTIFICADA";

    private final LicenciaEmitidaRepository licenciaRepository;

    @Override
    public ResultadoCondicion evaluar(Expediente expediente, LocalDate fechaEvaluacion) {
        return licenciaRepository.findByExpedienteId(expediente.getId())
                .filter(licencia -> licencia.estaNotificada())
                .map(licencia -> ResultadoCondicion.cumplida(CODIGO,
                        "Licencia notificada el " + licencia.getFechaNotificacion().toLocalDate()
                                + " por " + licencia.getMedioNotificacion()))
                .orElse(ResultadoCondicion.pendiente(CODIGO, "La licencia aún no ha sido notificada al administrado."));
    }
}
