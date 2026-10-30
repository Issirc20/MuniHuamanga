package pe.gob.munihuamanga.licencias.expedientes.itse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.common.enums.CanalRecepcion;
import pe.gob.munihuamanga.licencias.common.enums.EstadoItse;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Etapa ITSE de un expediente: recepción de los cuatro anexos firmados por Defensa Civil,
 * verificación formal por la oficina de licencias y liquidación de la tasa.
 */
@Entity
@Table(name = "itse_expedientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteItse {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "expediente_id", nullable = false, unique = true)
    private UUID expedienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoItse estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", length = 20)
    private CanalRecepcion canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_declarado", length = 20)
    private NivelRiesgo nivelDeclarado;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_oficial", length = 20)
    private NivelRiesgo nivelOficial;

    @Column(name = "monto_liquidado", precision = 10, scale = 2)
    private BigDecimal montoLiquidado;

    @Column(name = "concepto_liquidacion", length = 200)
    private String conceptoLiquidacion;

    @Column(name = "fecha_liquidacion")
    private LocalDateTime fechaLiquidacion;

    @Column(name = "liquidacion_definitiva", nullable = false)
    @Builder.Default
    private boolean liquidacionDefinitiva = false;

    @Column(name = "cargado_por_id")
    private UUID cargadoPorId;

    @Column(name = "cargado_por", length = 150)
    private String cargadoPor;

    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga;

    @Column(name = "comentario_carga", length = 500)
    private String comentarioCarga;

    @Column(name = "validado_por_id")
    private UUID validadoPorId;

    @Column(name = "validado_por", length = 150)
    private String validadoPor;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "comentario_validacion", length = 500)
    private String comentarioValidacion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Version
    @Column(name = "version")
    private Long version;
}
