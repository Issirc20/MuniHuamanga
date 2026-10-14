package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para Establecimientos vinculados a Cuentas (M02).
 * Incluye advertencia explícita de que la ubicación no constituye zonificación aprobada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstablecimientoDto {

    private UUID id;
    private UUID cuentaId;
    private String codigoEstablecimiento;
    private String nombreComercial;
    private String direccion;
    private String distrito;
    private String provincia;
    private String departamento;
    private String referencia;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String ciiuCodigo;
    private String giroPrincipal;
    private BigDecimal areaMetrosCuadrados;
    private String advertenciaZonificacion;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaModificacion;
}
