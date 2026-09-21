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
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad HistorialEstado para auditoría inmutable de transiciones de estado.
 */
@Entity
@Table(name = "historial_estados")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstado {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "expediente_id", nullable = false)
    private UUID expedienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoExpediente estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoExpediente estadoNuevo;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
