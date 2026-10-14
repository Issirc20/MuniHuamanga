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
public class HistorialResolucionDto {

    private UUID id;
    private UUID resolucionId;
    private String accion;
    private EstadoResolucion estadoAnterior;
    private EstadoResolucion estadoNuevo;
    private String actor;
    private String motivoObservaciones;
    private LocalDateTime fechaRegistro;
}
