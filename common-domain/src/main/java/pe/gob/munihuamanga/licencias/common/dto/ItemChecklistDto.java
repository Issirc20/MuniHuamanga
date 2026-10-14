package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoChecklist;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemChecklistDto {
    private String codigo;
    private String descripcion;
    private EstadoChecklist estado;
    private String motivo;
    private String fuenteEvidencia;
}
