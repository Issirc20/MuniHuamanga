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
public class SolicitarFirmaDto {

    @Size(max = 250, message = "El motivo de la firma no debe superar los 250 caracteres")
    private String motivoFirma;

    private String pinAutorizacion;

    private String idTransaccionRemota;
}
