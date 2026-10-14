package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmitirLicenciaDto {

    @NotNull(message = "El identificador de la resolución firmada es obligatorio")
    private UUID resolucionId;

    private TipoVigenciaLicencia tipoVigencia;

    private Integer plazoMeses;

    @Size(max = 1000, message = "Las observaciones de emisión no deben superar los 1000 caracteres")
    private String observacionesEmision;
}
