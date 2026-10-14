package pe.gob.munihuamanga.licencias.expedientes.licencias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para la auditoría de consultas móviles de inspectores (M10 - Incremento I09).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auditoria_consultas_inspeccion")
public class AuditoriaConsultaInspeccion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licencia_id", nullable = false)
    private LicenciaEmitida licencia;

    @Column(name = "token_qr", nullable = false, length = 100)
    private String tokenQr;

    @Column(name = "inspector_username", nullable = false, length = 100)
    private String inspectorUsername;

    @Column(name = "fecha_consulta", nullable = false)
    private LocalDateTime fechaConsulta;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "resultado", nullable = false, length = 50)
    private String resultado;
}
