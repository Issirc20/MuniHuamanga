package pe.gob.munihuamanga.licencias.expedientes.itse.model;

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
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.common.enums.TipoObservacionItse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Observación formulada por la oficina de licencias a un anexo ITSE.
 */
@Entity
@Table(name = "itse_observaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacionItse {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "expediente_itse_id", nullable = false)
    private UUID expedienteItseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "anexo", nullable = false, length = 20)
    private TipoAnexoItse anexo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoObservacionItse tipo;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "observado_por_id")
    private UUID observadoPorId;

    @Column(name = "observado_por", nullable = false, length = 150)
    private String observadoPor;

    @Column(name = "fecha_observacion", nullable = false)
    private LocalDateTime fechaObservacion;

    @Column(name = "atendida", nullable = false)
    @Builder.Default
    private boolean atendida = false;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;
}
