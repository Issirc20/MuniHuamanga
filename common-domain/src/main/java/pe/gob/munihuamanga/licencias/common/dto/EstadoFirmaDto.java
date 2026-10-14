package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoFirmaDto {

    private UUID resolucionId;
    private String numeroResolucion;
    private EstadoResolucion estadoResolucion;
    private boolean servicioFirmaDisponible;
    private String mensaje;
    private String firmanteNombre;
    private String firmanteCargo;
    private LocalDateTime fechaFirma;
    private String hashDocumento;
}
