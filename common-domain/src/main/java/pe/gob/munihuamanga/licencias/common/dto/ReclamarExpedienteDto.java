package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReclamarExpedienteDto {

    @NotNull(message = "El identificador de la cuenta reclamante es obligatorio")
    private UUID cuentaId;

    @NotBlank(message = "Debe proporcionar evidencia de titularidad (ej. copia DNI, partida electrónica, o código de trámite)")
    private String evidenciaTitularidad;

    private String motivo;
}
