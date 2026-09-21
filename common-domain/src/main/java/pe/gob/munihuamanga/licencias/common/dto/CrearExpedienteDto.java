package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearExpedienteDto {

    @NotNull(message = "El identificador del solicitante es obligatorio")
    private UUID solicitanteId;

    @NotBlank(message = "El nombre del titular o representante es obligatorio")
    private String nombreTitular;

    @NotBlank(message = "El documento de identidad (DNI/RUC) es obligatorio")
    private String documentoIdentidad;

    private String razonSocial;

    @NotBlank(message = "El nombre comercial es obligatorio")
    private String nombreComercial;

    @NotBlank(message = "El giro del negocio es obligatorio")
    private String giroNegocio;

    @NotBlank(message = "La dirección del establecimiento es obligatoria")
    private String direccionEstablecimiento;

    @DecimalMin(value = "1.0", message = "El área en metros cuadrados debe ser mayor a 0")
    private BigDecimal areaMetrosCuadrados;
}
