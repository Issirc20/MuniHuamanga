package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import pe.gob.munihuamanga.licencias.common.enums.TipoObservacion;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para la consulta ciudadana y administrativa de observaciones (Incremento I05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacionExpedienteDto {

    private UUID id;
    private UUID expedienteId;
    private UUID documentoId;
    private TipoObservacion tipoObservacion;
    private String requisitoAfectado;
    private String motivo;
    private String fundamentoLegal;
    private LocalDateTime fechaObservacion;
    private String observadorUsername;
    private Integer plazoDiasHabiles;
    private LocalDateTime fechaLimiteSubsanacion;
    private Boolean subsanada;
    private LocalDateTime fechaSubsanacion;
    private UUID documentoSubsanacionId;
    private String detalleSubsanacion;
}
