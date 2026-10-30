package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.MedioNotificacion;

/**
 * Registro de la notificación de la licencia al administrado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarNotificacionLicenciaDto {

    @NotNull(message = "Debe indicar el medio de notificación.")
    private MedioNotificacion medio;

    @Size(max = 200, message = "La referencia de la constancia no debe exceder los 200 caracteres.")
    private String constancia;
}
