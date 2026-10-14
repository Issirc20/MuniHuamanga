package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistAdministrativoDto {
    private UUID expedienteId;
    private String numeroExpediente;
    private LocalDateTime fechaEvaluacion;
    private List<ItemChecklistDto> items;
    private boolean aptoParaInformeFavorable;
    private String resumen;
}
