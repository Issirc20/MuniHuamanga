package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteResponseDto {

    private UUID id;
    private String numeroTramite;
    private UUID solicitanteId;
    private String nombreTitular;
    private String documentoIdentidad;
    private String razonSocial;
    private String nombreComercial;
    private String giroNegocio;
    private String direccionEstablecimiento;
    private BigDecimal areaMetrosCuadrados;
    private String correoElectronico;
    private String telefono;

    private EstadoExpediente estado;
    private NivelRiesgo nivelRiesgo;
    private String tipoItse; // "ITSE_POSTERIOR" o "ITSE_PREVIA"
    private BigDecimal montoTasa;
    private String voucherId;
    private String licenciaQrCode;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLimite;
    private Long diasHabilesRestantes;
    private Boolean alertaVencimiento;
}
