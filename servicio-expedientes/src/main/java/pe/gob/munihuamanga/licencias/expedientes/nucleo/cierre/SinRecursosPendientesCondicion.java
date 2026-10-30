package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.LocalDate;

/**
 * No debe existir ningún recurso administrativo en trámite contra la resolución.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class SinRecursosPendientesCondicion implements CondicionCierre {

    static final String CODIGO = "SIN_RECURSOS_PENDIENTES";

    private final ConsultaRecursosAdministrativos consultaRecursos;

    @Override
    public ResultadoCondicion evaluar(Expediente expediente, LocalDate fechaEvaluacion) {
        return consultaRecursos.existenRecursosPendientes(expediente.getId())
                ? ResultadoCondicion.pendiente(CODIGO, "Existen recursos administrativos en trámite.")
                : ResultadoCondicion.cumplida(CODIGO, "No existen recursos administrativos en trámite.");
    }
}
