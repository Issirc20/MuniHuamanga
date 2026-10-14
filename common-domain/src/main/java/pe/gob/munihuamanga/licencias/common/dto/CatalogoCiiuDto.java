package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

/**
 * DTO para el Catálogo CIIU administrable de giros y actividades económicas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoCiiuDto {
    private String codigo;
    private String descripcion;
    private String giroPrincipal;
    private NivelRiesgo nivelRiesgoSugerido;
    private Boolean activo;
}
