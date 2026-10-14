package pe.gob.munihuamanga.licencias.expedientes.zonificacion.model;

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
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para el registro formal de la evaluación técnica de zonificación y compatibilidad de uso (M07 - I06).
 */
@Entity
@Table(name = "evaluaciones_zonificacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionZonificacion {

    @Id
    private UUID id;

    @Column(name = "expediente_id", nullable = false)
    private UUID expedienteId;

    @Column(name = "establecimiento_id")
    private UUID establecimientoId;

    @Column(name = "direccion_declarada", nullable = false, length = 255)
    private String direccionDeclarada;

    @Column(name = "latitud_declarada", precision = 10, scale = 7)
    private BigDecimal latitudDeclarada;

    @Column(name = "longitud_declarada", precision = 10, scale = 7)
    private BigDecimal longitudDeclarada;

    @Column(name = "codigo_ciiu", length = 20)
    private String codigoCiiu;

    @Column(name = "giro_comercial", nullable = false, length = 150)
    private String giroComercial;

    @Column(name = "zonificacion_pdu", length = 50)
    private String zonificacionPdu;

    @Column(name = "referencia_zonificacion", nullable = false, length = 255)
    private String referenciaZonificacion;

    @Column(name = "fuente_sustento_tecnico", nullable = false, length = 255)
    private String fuenteSustentoTecnico;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 40)
    private ResultadoZonificacion resultado;

    @Column(name = "evaluador_id")
    private UUID evaluadorId;

    @Column(name = "evaluador_nombre", length = 150)
    private String evaluadorNombre;

    @Column(name = "fecha_evaluacion")
    private LocalDateTime fechaEvaluacion;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "documento_sustentatorio_id")
    private UUID documentoSustentatorioId;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
