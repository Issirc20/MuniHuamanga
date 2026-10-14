package pe.gob.munihuamanga.licencias.expedientes.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para el registro y ciclo de vida de validación manual de pagos SAT (M05 - I06).
 */
@Entity
@Table(name = "pagos_expediente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoExpediente {

    @Id
    private UUID id;

    @Column(name = "expediente_id", nullable = false)
    private UUID expedienteId;

    @Column(name = "codigo_comprobante", nullable = false, unique = true, length = 60)
    private String codigoComprobante;

    @Column(name = "fecha_declarada", nullable = false)
    private LocalDate fechaDeclarada;

    @Column(name = "monto_declarado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoDeclarado;

    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "observaciones_administrado", columnDefinition = "TEXT")
    private String observacionesAdministrado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoValidacionPago estado;

    @Column(name = "validador_id")
    private UUID validadorId;

    @Column(name = "validador_nombre", length = 150)
    private String validadorNombre;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "motivo_observacion_rechazo", columnDefinition = "TEXT")
    private String motivoObservacionRechazo;

    @Column(name = "numero_operacion_sat", length = 60)
    private String numeroOperacionSat;

    @Column(name = "fecha_pago_real")
    private LocalDate fechaPagoReal;

    @Column(name = "monto_validado", precision = 10, scale = 2)
    private BigDecimal montoValidado;

    @Column(name = "tasa_tupa_esperada", precision = 10, scale = 2)
    private BigDecimal tasaTupaEsperada;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
