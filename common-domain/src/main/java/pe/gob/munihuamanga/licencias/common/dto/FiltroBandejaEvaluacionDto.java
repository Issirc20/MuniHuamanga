package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiltroBandejaEvaluacionDto {
    private EstadoExpediente estadoExpediente;
    private EstadoValidacionPago estadoPago;
    private ResultadoZonificacion resultadoZonificacion;
    private NivelRiesgo nivelRiesgo;
    private String busqueda;
}
