package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para el registro de solicitudes en Mesa de Partes Virtual con validaciones legales peruanas (DNI/RUC).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearExpedienteDto {

    @NotNull(message = "El identificador del solicitante es obligatorio")
    private UUID solicitanteId;

    @NotBlank(message = "El nombre del titular o representante es obligatorio")
    private String nombreTitular;

    @NotBlank(message = "El documento de identidad es obligatorio")
    @Pattern(
            regexp = "^([0-9]{8}|(10|20)[0-9]{9})$",
            message = "El documento debe ser un DNI válido de 8 dígitos o un RUC válido de 11 dígitos iniciado con 10 o 20"
    )
    private String documentoIdentidad;

    private String razonSocial;

    @NotBlank(message = "El nombre comercial es obligatorio")
    private String nombreComercial;

    @NotBlank(message = "El giro del negocio es obligatorio")
    private String giroNegocio;

    @NotBlank(message = "La dirección del establecimiento es obligatoria")
    private String direccionEstablecimiento;

    @NotNull(message = "El área en metros cuadrados es obligatoria")
    @DecimalMin(value = "1.0", message = "El área del establecimiento debe ser de al menos 1.0 m²")
    private BigDecimal areaMetrosCuadrados;

    @Email(message = "El formato del correo electrónico de notificación no es válido")
    private String correoElectronico;

    @Pattern(regexp = "^(9[0-9]{8})?$", message = "El teléfono debe ser un número celular peruano de 9 dígitos")
    private String telefono;
}
