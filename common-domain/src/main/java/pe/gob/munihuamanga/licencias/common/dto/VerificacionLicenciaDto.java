package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionLicenciaDto {

    private String numeroLicencia;
    private String numeroTramite;
    private String titular;
    private String documentoIdentidad;
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
