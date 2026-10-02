package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para la visualización y transferencia del Tarifario TUPA (Fase 04 Sprint 4-D).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaTupaDto {

    private UUID id;
    private String codigoTupa;
    private NivelRiesgo nivelRiesgo;
    private String concepto;
    private BigDecimal montoTotal;
    private BigDecimal derechoTramite;
    private BigDecimal costoItse;
    private String baseLegal;
    private boolean activo;
    private LocalDateTime fechaActualizacion;
    private String usuarioModificacion;
}
