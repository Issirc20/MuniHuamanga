package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resultado de una actuación que modificó el estado del expediente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoTransicionDto {

    private UUID expedienteId;
    private String numeroTramite;
    private EstadoExpediente estadoAnterior;
    private EstadoExpediente estadoNuevo;
    private String evento;
    private LocalDateTime fecha;
}
