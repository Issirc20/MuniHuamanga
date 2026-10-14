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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para la trazabilidad y auditoría inmutable de decisiones tomadas sobre pagos (M05 - I06).
 */
@Entity
@Table(name = "historial_decisiones_pago")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialDecisionPago {

    @Id
    private UUID id;

    @Column(name = "pago_id", nullable = false)
    private UUID pagoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 40)
    private EstadoValidacionPago estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 40)
    private EstadoValidacionPago estadoNuevo;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "fecha_decision", nullable = false)
    private LocalDateTime fechaDecision;
}
