package pe.gob.munihuamanga.licencias.expedientes.evaluacion.model;

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
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para el registro inmutable de decisiones y transiciones del informe administrativo.
 */
@Entity
@Table(name = "historial_informes_administrativos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialInformeAdministrativo {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "informe_id", nullable = false)
    private InformeAdministrativo informe;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoInforme estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoInforme estadoNuevo;

    @Column(name = "responsable", nullable = false, length = 200)
    private String responsable;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;
}
