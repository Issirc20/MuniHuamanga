package pe.gob.munihuamanga.licencias.expedientes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Objeto de valor embebido para las condiciones de seguridad en edificación (Anexo 4).
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anexo4Condiciones {

    @Column(name = "a4_area_terreno", precision = 10, scale = 2)
    private BigDecimal areaTerreno;

    @Column(name = "a4_area_piso_1", precision = 10, scale = 2)
    private BigDecimal areaPiso1;

    @Column(name = "a4_area_piso_2", precision = 10, scale = 2)
    private BigDecimal areaPiso2;

    @Column(name = "a4_area_piso_3", precision = 10, scale = 2)
    private BigDecimal areaPiso3;

    @Column(name = "a4_area_piso_4", precision = 10, scale = 2)
    private BigDecimal areaPiso4;

    @Column(name = "a4_area_otros_pisos", precision = 10, scale = 2)
    private BigDecimal areaOtrosPisos;

    @Column(name = "a4_area_techada_total", precision = 10, scale = 2)
    private BigDecimal areaTechadaTotal;

    @Column(name = "a4_area_ocupada_total", precision = 10, scale = 2)
    private BigDecimal areaOcupadaTotal;

    @Column(name = "a4_aforo_personas")
    private Integer aforoPersonas;

    @Column(name = "a4_antiguedad_edificacion")
    private Integer antiguedadEdificacionAnios;

    @Column(name = "a4_antiguedad_giro")
    private Integer antiguedadGiroAnios;

    @Builder.Default
    @Column(name = "a4_no_proceso_construccion")
    private Boolean noEnProcesoConstruccion = true;

    @Builder.Default
    @Column(name = "a4_cuenta_servicios_basicos")
    private Boolean cuentaServiciosBasicos = true;

    @Builder.Default
    @Column(name = "a4_cuenta_mobiliario_basico")
    private Boolean cuentaMobiliarioBasico = true;

    @Builder.Default
    @Column(name = "a4_tiene_equipos_instalados")
    private Boolean tieneEquiposInstalados = true;

    @Builder.Default
    @Column(name = "a4_medios_evacuacion_libres")
    private Boolean mediosEvacuacionLibres = true;

    @Builder.Default
    @Column(name = "a4_senalizacion_seguridad")
    private Boolean senalizacionSeguridad = true;

    @Builder.Default
    @Column(name = "a4_luces_emergencia")
    private Boolean lucesEmergenciaOperativas = true;

    @Builder.Default
    @Column(name = "a4_tablero_electrico_protegido")
    private Boolean tableroElectricoProtegido = true;

    @Builder.Default
    @Column(name = "a4_interruptores_diferenciales")
    private Boolean interruptoresDiferenciales = true;

    @Builder.Default
    @Column(name = "a4_pozo_tierra_vigente")
    private Boolean pozoTierraVigente = true;

    @Builder.Default
    @Column(name = "a4_extintores_operativos")
    private Boolean extintoresOperativos = true;

    @Builder.Default
    @Column(name = "a4_estructuras_sin_colapso")
    private Boolean estructurasSinRiesgoColapso = true;

    @Builder.Default
    @Column(name = "a4_cables_protegidos_pvc")
    private Boolean cablesProtegidosTubosPvc = true;
}
