package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstadoDto {

    private UUID id;
    private UUID expedienteId;
    private EstadoExpediente estadoAnterior;
    private EstadoExpediente estadoNuevo;
    private String usuario;
    private String motivo;
    private LocalDateTime fecha;
}
