package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.CanalRecepcion;
import pe.gob.munihuamanga.licencias.common.enums.EstadoItse;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Situación de la etapa ITSE de un expediente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteItseDto {

    private UUID expedienteId;
    private String numeroTramite;
    private EstadoItse estado;
    private CanalRecepcion canal;
    private NivelRiesgo nivelDeclarado;
    private NivelRiesgo nivelOficial;
    private List<AnexoItseDto> anexos;
    private List<ObservacionItseDto> observaciones;
    private LiquidacionDto liquidacion;
    private String cargadoPor;
    private LocalDateTime fechaCarga;
    private String validadoPor;
    private LocalDateTime fechaValidacion;
}
