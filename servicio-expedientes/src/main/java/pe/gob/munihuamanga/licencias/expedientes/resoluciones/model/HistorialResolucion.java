package pe.gob.munihuamanga.licencias.expedientes.resoluciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historial_resoluciones")
public class HistorialResolucion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolucion_id", nullable = false)
    private ResolucionAdministrativa resolucion;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoResolucion estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoResolucion estadoNuevo;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "motivo_observaciones", columnDefinition = "TEXT")
    private String motivoObservaciones;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
