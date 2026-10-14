package pe.gob.munihuamanga.licencias.expedientes.rectificacion.model;

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
import pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para la solicitud de rectificación de coordenadas geográficas (Criterio CA-08).
 */
@Entity
@Table(name = "solicitudes_rectificacion_ubicacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRectificacionUbicacion {

    @Id
    private UUID id;

    @Column(name = "establecimiento_id", nullable = false)
    private UUID establecimientoId;

    @Column(name = "expediente_id")
    private UUID expedienteId;

    @Column(name = "latitud_original", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitudOriginal;

    @Column(name = "longitud_original", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitudOriginal;

    @Column(name = "latitud_propuesta", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitudPropuesta;

    @Column(name = "longitud_propuesta", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitudPropuesta;

    @Column(name = "distancia_metros_aprox")
    private Double distanciaMetrosAprox;

    @Column(name = "justificacion", nullable = false, columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "documento_sustento_id")
    private UUID documentoSustentoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoRectificacionUbicacion estado;

    @Column(name = "solicitante", nullable = false, length = 200)
    private String solicitante;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "evaluador", length = 200)
    private String evaluador;

    @Column(name = "fecha_evaluacion")
    private LocalDateTime fechaEvaluacion;

    @Column(name = "motivo_respuesta", columnDefinition = "TEXT")
    private String motivoRespuesta;

    @Column(name = "es_traslado_fisico", nullable = false)
    @Builder.Default
    private Boolean esTrasladoFisico = false;
}
