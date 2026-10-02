package pe.gob.munihuamanga.licencias.expedientes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que representa una tasa administrativa del TUPA municipal (Fase 04 Sprint 4-D).
 */
@Entity
@Table(name = "tarifas_tupa")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaTupa {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "codigo_tupa", nullable = false, unique = true, length = 50)
    private String codigoTupa;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", nullable = false, unique = true, length = 20)
    private NivelRiesgo nivelRiesgo;

    @Column(name = "concepto", nullable = false, length = 255)
    private String concepto;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "derecho_tramite", nullable = false, precision = 10, scale = 2)
    private BigDecimal derechoTramite;

    @Column(name = "costo_itse", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoItse;

    @Column(name = "base_legal", length = 255)
    private String baseLegal;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_actualizacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @Column(name = "usuario_modificacion", length = 100)
    private String usuarioModificacion;
}
