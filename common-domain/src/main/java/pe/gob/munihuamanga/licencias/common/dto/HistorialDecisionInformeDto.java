package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialDecisionInformeDto {
    private UUID id;
    private UUID informeId;
    private String accion;
    private EstadoInforme estadoAnterior;
    private EstadoInforme estadoNuevo;
    private String responsable;
    private LocalDateTime fecha;
    private String motivo;
}
