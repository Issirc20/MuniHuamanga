package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

/**
 * DTO para el registro seguro de Cuentas Ciudadanas (DNI) e Institucionales (RUC) - M01.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroCuentaDto {

    @NotNull(message = "El tipo de persona es obligatorio (NATURAL o JURIDICA)")
    private TipoPersona tipoPersona;

    @NotNull(message = "El tipo de documento es obligatorio (DNI o RUC)")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento de identidad es obligatorio")
    @Pattern(
            regexp = "^([0-9]{8}|(10|20)[0-9]{9})$",
            message = "El documento debe ser un DNI válido de 8 dígitos o un RUC válido de 11 dígitos iniciado con 10 o 20"
    )
    private String numeroDocumento;

    @NotBlank(message = "La razón social o nombres y apellidos completos son obligatorios")
    @Size(min = 3, max = 200, message = "La razón social o nombre debe tener entre 3 y 200 caracteres")
    private String razonSocialONombre;

    @NotBlank(message = "El correo electrónico es obligatorio para notificaciones y verificación")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    @Pattern(regexp = "^(9[0-9]{8})?$", message = "El teléfono debe ser un celular peruano de 9 dígitos")
    private String telefono;

    private String direccionFiscal;

    @NotBlank(message = "La contraseña de acceso es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    // Campos complementarios para personas jurídicas (RUC)
    private String nombreRepresentanteLegal;
    private String dniRepresentanteLegal;
    private String partidaSunarp;
}
