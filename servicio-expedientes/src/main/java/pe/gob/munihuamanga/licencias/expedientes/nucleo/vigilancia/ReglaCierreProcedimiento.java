package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.dto.AccionPropuestaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.TipoAccionPropuesta;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre.PoliticaCierreProcedimiento;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Detecta los procedimientos con licencia emitida que cumplen todas las condiciones de cierre.
 */
@Component
@RequiredArgsConstructor
public class ReglaCierreProcedimiento implements ReglaVencimiento {

    private final PoliticaCierreProcedimiento politicaCierre;

    @Override
    public boolean aplicaA(EstadoExpediente estado) {
        return estado == EstadoExpediente.EMITIDO;
    }

    @Override
    public Optional<AccionPropuestaDto> evaluar(Expediente expediente, LocalDate fechaEvaluacion) {
        if (!politicaCierre.puedeConcluir(expediente, fechaEvaluacion)) {
            return Optional.empty();
        }
        return Optional.of(AccionPropuestaDto.builder()
                .expedienteId(expediente.getId())
                .numeroTramite(expediente.getNumeroTramite())
                .estado(expediente.getEstado())
                .regla("CIERRE_PROCEDIMIENTO")
                .accion(TipoAccionPropuesta.CONCLUIR_PROCEDIMIENTO)
                .descripcion("Se cumplieron todas las condiciones de cierre del procedimiento.")
                .fechaReferencia(fechaEvaluacion)
                .build());
    }
}
