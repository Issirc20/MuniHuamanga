package pe.gob.munihuamanga.licencias.expedientes.documentos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para registrar observaciones técnicas y documentales (Ley N° 27444 / TUPA MPH).
 */
@Entity
@Table(name = "observaciones_expediente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacionExpediente {

    @Id
    private UUID id;

    @Column(name = "expediente_id", nullable = false)
    private UUID expedienteId;

    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "requisito_afectado", nullable = false, length = 100)
    private String requisitoAfectado;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "fundamento_legal", length = 255)
    private String fundamentoLegal;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "tipo_observacion", length = 50)
    private pe.gob.munihuamanga.licencias.common.enums.TipoObservacion tipoObservacion;

    @Column(name = "fecha_observacion", nullable = false)
    private LocalDateTime fechaObservacion;

    @Column(name = "observador_id")
    private UUID observadorId;

    @Column(name = "observador_username", length = 100)
    private String observadorUsername;

    @Column(name = "plazo_dias_habiles", nullable = false)
    private Integer plazoDiasHabiles;

    @Column(name = "fecha_limite_subsanacion")
    private LocalDateTime fechaLimiteSubsanacion;

    @Column(name = "subsanada", nullable = false)
    private Boolean subsanada;

    @Column(name = "fecha_subsanacion")
    private LocalDateTime fechaSubsanacion;

    @Column(name = "documento_subsanacion_id")
    private UUID documentoSubsanacionId;

    @Column(name = "detalle_subsanacion", columnDefinition = "TEXT")
    private String detalleSubsanacion;
}
