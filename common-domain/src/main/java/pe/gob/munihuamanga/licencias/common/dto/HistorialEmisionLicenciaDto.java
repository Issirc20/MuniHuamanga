package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEmisionLicenciaDto {

    private UUID id;
    private UUID licenciaId;
    private String accion;
    private EstadoLicencia estadoAnterior;
    private EstadoLicencia estadoNuevo;
    private String actor;
    private String detalle;
    private LocalDateTime fechaRegistro;
}
