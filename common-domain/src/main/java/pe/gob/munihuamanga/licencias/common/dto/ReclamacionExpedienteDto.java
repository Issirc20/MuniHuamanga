package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoReclamacion;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReclamacionExpedienteDto {

    private UUID id;
    private UUID expedienteId;
    private String numeroTramite;
    private UUID cuentaId;
    private String titularNombre;
    private String solicitanteUsername;
    private EstadoReclamacion estado;
    private String evidenciaTitularidad;
    private String motivo;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private UUID evaluadorId;
    private String motivoResolucion;
}
