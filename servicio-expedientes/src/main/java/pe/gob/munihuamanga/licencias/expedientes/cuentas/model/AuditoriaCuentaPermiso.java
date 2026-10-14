package pe.gob.munihuamanga.licencias.expedientes.cuentas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pista de auditoría inmutable para cambios de permisos y operadores en Cuentas (M01).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auditoria_cuenta_permisos")
public class AuditoriaCuentaPermiso {

    @Id
    private UUID id;

    @Column(name = "cuenta_id", nullable = false)
    private UUID cuentaId;

    @Column(name = "operador_id", nullable = false)
    private UUID operadorId;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "permisos_anteriores", length = 255)
    private String permisosAnteriores;

    @Column(name = "permisos_nuevos", length = 255)
    private String permisosNuevos;

    @Column(name = "usuario_responsable", nullable = false, length = 100)
    private String usuarioResponsable;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;
}
