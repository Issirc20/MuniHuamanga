package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRectificacionUbicacionDto {
    private UUID id;
    private UUID establecimientoId;
    private String establecimientoNombre;
    private UUID expedienteId;
    private BigDecimal latitudOriginal;
    private BigDecimal longitudOriginal;
    private BigDecimal latitudPropuesta;
    private BigDecimal longitudPropuesta;
    private Double distanciaMetrosAprox;
    private String justificacion;
    private UUID documentoSustentoId;
    private EstadoRectificacionUbicacion estado;
    private String solicitante;
    private LocalDateTime fechaSolicitud;
    private String evaluador;
    private LocalDateTime fechaEvaluacion;
    private String motivoRespuesta;
    private Boolean esTrasladoFisico;
}
