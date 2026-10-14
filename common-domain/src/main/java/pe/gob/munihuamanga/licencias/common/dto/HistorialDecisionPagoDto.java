package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para la consulta del historial de decisiones y trazabilidad de un pago (M05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialDecisionPagoDto {

    private UUID id;
    private UUID pagoId;
    private EstadoValidacionPago estadoAnterior;
    private EstadoValidacionPago estadoNuevo;
    private UUID usuarioId;
    private String usuarioNombre;
    private String motivo;
    private LocalDateTime fechaDecision;
}
