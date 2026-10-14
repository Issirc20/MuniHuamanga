package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para la consulta ampliada de licencias destinada a Inspectores Municipales
 * y funcionarios de la Gerencia de Licencias (ERS-MH-V2-001 RF-VER-007 / DAS DA-05 / CA-10).
 * Incluye georreferenciación en mapa, zonificación y datos de verificación física
 * sin funciones de fiscalización, actas ni sanciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionAmpliadaDto {

    private String numeroLicencia;
    private String numeroTramite;
    private String titular;
    private String documentoIdentidad;
    private String razonSocial;
    private String nombreComercial;
    private String giro;
    private String ciiuCodigo;
    private String direccion;
    private String zonificacion;
    private String distrito;
    private String provincia;
    private String departamento;
    private String referenciaUbicacion;
    private BigDecimal areaMetrosCuadrados;
    private Integer aforoPersonas;
    private NivelRiesgo nivelRiesgo;
    private String estado;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaAprobacion;
    private String categoriaEstablecimiento;
    private String horarioFuncionamiento;
    private boolean valida;
    private String mensajeVerificacion;

    // Coordenadas geográficas para verificación y comparación en campo (WGS84)
    private Double latitud;
    private Double longitud;
}
