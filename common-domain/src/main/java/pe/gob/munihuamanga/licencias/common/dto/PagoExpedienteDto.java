package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para el registro y estado de validación manual de pagos (M05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoExpedienteDto {

    private UUID id;
    private UUID expedienteId;
    private String numeroTramite;
    private String codigoComprobante;
    private LocalDate fechaDeclarada;
    private BigDecimal montoDeclarado;
    private UUID documentoId;
    private String observacionesAdministrado;
    private EstadoValidacionPago estado;
    private UUID validadorId;
    private String validadorNombre;
    private LocalDateTime fechaValidacion;
    private String motivoObservacionORechazo;
    private String numeroOperacionSat;
    private LocalDate fechaPagoReal;
    private BigDecimal montoValidado;
    private BigDecimal tasaTupaEsperada;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
}
