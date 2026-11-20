package pe.gob.munihuamanga.licencias.expedientes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA unificada para auditoría inmutable de operaciones del sistema (M12).
 * Consolida consultas QR de inspectores, emisión de actos resolutivos y mutaciones críticas.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auditoria_operaciones", indexes = {
    @Index(name = "idx_audit_op_modulo", columnList = "modulo"),
    @Index(name = "idx_audit_op_entidad", columnList = "entidad_id"),
    @Index(name = "idx_audit_op_usuario", columnList = "usuario")
})
public class AuditoriaOperacion {

    @Id
    private UUID id;

    @Column(name = "modulo", nullable = false, length = 50)
    private String modulo;

    @Column(name = "operacion", nullable = false, length = 50)
    private String operacion;

    @Column(name = "entidad_id")
    private UUID entidadId;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "resultado", nullable = false, length = 50)
    private String resultado;

    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;
}
