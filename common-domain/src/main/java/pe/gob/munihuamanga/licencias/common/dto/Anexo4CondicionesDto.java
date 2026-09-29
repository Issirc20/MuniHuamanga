package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO que encapsula la información del Anexo 4:
 * Declaración Jurada de Cumplimiento de las Condiciones de Seguridad en la Edificación
 * (Para establecimientos clasificados con nivel de riesgo bajo o medio - D.S. N° 002-2018-PCM).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anexo4CondicionesDto {

    // I. Datos de dimensionamiento y capacidad
    private BigDecimal areaTerreno;
    private BigDecimal areaPiso1;
    private BigDecimal areaPiso2;
    private BigDecimal areaPiso3;
    private BigDecimal areaPiso4;
    private BigDecimal areaOtrosPisos;
    private BigDecimal areaTechadaTotal;
    private BigDecimal areaOcupadaTotal;
    private Integer aforoPersonas;
    private Integer antiguedadEdificacionAnios;
    private Integer antiguedadGiroAnios;

    // III. Condiciones básicas de la edificación
    @Builder.Default
    private Boolean noEnProcesoConstruccion = true;
    @Builder.Default
    private Boolean cuentaServiciosBasicos = true;
    @Builder.Default
    private Boolean cuentaMobiliarioBasico = true;
    @Builder.Default
    private Boolean tieneEquiposInstalados = true;

    // IV. Cumplimiento de condiciones de seguridad
    @Builder.Default
    private Boolean mediosEvacuacionLibres = true;
    @Builder.Default
    private Boolean senalizacionSeguridad = true;
    @Builder.Default
    private Boolean lucesEmergenciaOperativas = true;
    @Builder.Default
    private Boolean tableroElectricoProtegido = true;
    @Builder.Default
    private Boolean interruptoresDiferenciales = true;
    @Builder.Default
    private Boolean pozoTierraVigente = true;
    @Builder.Default
    private Boolean extintoresOperativos = true;
    @Builder.Default
    private Boolean estructurasSinRiesgoColapso = true;
    @Builder.Default
    private Boolean cablesProtegidosTubosPvc = true;
}
