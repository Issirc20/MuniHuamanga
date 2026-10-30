package pe.gob.munihuamanga.licencias.expedientes.security.sesion;

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
 * Sesión de acceso. Conserva el resumen criptográfico del token de renovación vigente,
 * que rota en cada renovación; la revocación de la sesión invalida de inmediato sus tokens de acceso.
 */
@Entity
@Table(name = "sesiones_usuario")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionUsuario {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_renovacion_hash", nullable = false, length = 64)
    private String tokenRenovacionHash;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_ultima_renovacion")
    private LocalDateTime fechaUltimaRenovacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "revocada", nullable = false)
    @Builder.Default
    private boolean revocada = false;

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;

    @Column(name = "motivo_revocacion", length = 100)
    private String motivoRevocacion;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "agente_usuario", length = 255)
    private String agenteUsuario;

    public boolean estaVigente(LocalDateTime momento) {
        return !revocada && fechaExpiracion.isAfter(momento);
    }
}
