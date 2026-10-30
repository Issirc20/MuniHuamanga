package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.common.enums.TipoObservacionItse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Observación registrada sobre un anexo ITSE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacionItseDto {

    private UUID id;
    private TipoAnexoItse anexo;
    private TipoObservacionItse tipo;
    private String motivo;
    private String observadoPor;
    private LocalDateTime fechaObservacion;
    private boolean atendida;
}
