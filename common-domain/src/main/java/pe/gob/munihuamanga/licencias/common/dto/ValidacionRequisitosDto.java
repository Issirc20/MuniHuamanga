package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionRequisitosDto {

    private UUID borradorId;
    private boolean aptoParaPresentacion;
    private List<String> camposFaltantes;
    private List<String> documentosFaltantes;
    private List<String> observaciones;
    private String mensajeResumen;
}
