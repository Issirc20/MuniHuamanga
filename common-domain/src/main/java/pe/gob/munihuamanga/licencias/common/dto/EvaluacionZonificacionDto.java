package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para la evaluación de zonificación y compatibilidad de uso (M07).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionZonificacionDto {

    private UUID id;
    private UUID expedienteId;
    private String numeroTramite;
    private UUID establecimientoId;
    private String direccionDeclarada;
    private BigDecimal latitudDeclarada;
    private BigDecimal longitudDeclarada;
    private String codigoCiiu;
    private String giroComercial;
    private String zonificacionPdu;
    private String referenciaZonificacion;
    private String fuenteSustentoTecnico;
    private ResultadoZonificacion resultado;
    private UUID evaluadorId;
    private String evaluadorNombre;
    private LocalDateTime fechaEvaluacion;
    private String observaciones;
    private UUID documentoSustentatorioId;
    private LocalDateTime fechaRegistro;
}
