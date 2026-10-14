package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.time.LocalDateTime;

/**
 * DTO para la verificación pública de licencias mediante escaneo de código QR (RNF-SEG-007 / CA-09).
 * En conformidad con la Corrección I02.1:
 * Se elimina completamente el DNI/RUC de la respuesta pública para proteger los datos personales (Ley N° 29733).
 * Los datos ampliados (incluyendo DNI/RUC completo) se reservan exclusivamente para funcionarios autorizados en VerificacionAmpliadaDto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionLicenciaDto {

    private String numeroLicencia;
    private String numeroTramite;
    private String titular;
    private String razonSocial;
    private String nombreComercial;
    private String giro;
    private String direccion;
    private NivelRiesgo nivelRiesgo;
    private String estado;
    private LocalDateTime fechaEmision;
    private boolean valida;
    private String mensajeVerificacion;
}
