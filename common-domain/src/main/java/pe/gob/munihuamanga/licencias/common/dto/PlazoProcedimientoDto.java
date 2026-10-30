package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Situación del plazo legal de evaluación de un expediente, reconstruida desde su historial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlazoProcedimientoDto {

    private UUID expedienteId;
    private String numeroTramite;
    private EstadoExpediente estado;
    private boolean iniciado;
    private LocalDate fechaInicio;
    private int diasPlazo;
    private int diasConsumidos;
    private int diasRestantes;
    private LocalDate fechaVencimientoEstimada;
    private boolean suspendido;
    private boolean concluido;
    private boolean vencido;
}
