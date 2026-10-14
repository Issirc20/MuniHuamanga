package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO para la presentación formal de la subsanación de observaciones por el administrado (Art. 136 Ley N° 27444).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubsanarObservacionesDto {

    @NotBlank(message = "Debe describir el detalle o justificación de las subsanaciones presentadas.")
    private String detalleSubsanacion;

    private List<UUID> observacionesIds;

    private String usuario;
}
