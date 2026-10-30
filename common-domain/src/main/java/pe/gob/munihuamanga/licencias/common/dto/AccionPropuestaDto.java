package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.TipoAccionPropuesta;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Acción propuesta por el vigilante de plazos para la bandeja de expedientes con plazo vencido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccionPropuestaDto {

    private UUID expedienteId;
    private String numeroTramite;
    private EstadoExpediente estado;
    private String regla;
    private TipoAccionPropuesta accion;
    private String descripcion;
    private LocalDate fechaReferencia;
    private int diasTranscurridos;
    private int diasLimite;
}
