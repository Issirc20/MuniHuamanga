package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprobarResolucionDto {

    @Size(max = 1000, message = "Las observaciones no deben superar los 1000 caracteres")
    private String observaciones;

    @Size(max = 150, message = "El cargo de la autoridad no debe superar los 150 caracteres")
    private String cargoAutoridad;
}
