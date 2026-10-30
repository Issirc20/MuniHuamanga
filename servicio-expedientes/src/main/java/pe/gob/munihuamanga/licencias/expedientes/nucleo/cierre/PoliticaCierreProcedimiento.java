package pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.LocalDate;
import java.util.List;

/**
 * Determina si un procedimiento con licencia emitida puede concluirse.
 * Se requiere que se cumplan todas las condiciones de cierre registradas en el contexto.
 */
@Component
@RequiredArgsConstructor
public class PoliticaCierreProcedimiento {

    private final List<CondicionCierre> condiciones;

    public List<ResultadoCondicion> evaluar(Expediente expediente, LocalDate fechaEvaluacion) {
        return condiciones.stream()
                .map(condicion -> condicion.evaluar(expediente, fechaEvaluacion))
                .toList();
    }

    public boolean puedeConcluir(Expediente expediente, LocalDate fechaEvaluacion) {
        return expediente.getEstado() == EstadoExpediente.EMITIDO
                && evaluar(expediente, fechaEvaluacion).stream().allMatch(ResultadoCondicion::cumplida);
    }
}
