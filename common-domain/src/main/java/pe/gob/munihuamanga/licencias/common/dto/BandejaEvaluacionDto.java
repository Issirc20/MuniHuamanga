package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BandejaEvaluacionDto {
    private UUID expedienteId;
    private String numeroExpediente;
    private String titularNombre;
    private String titularDocumento;
    private UUID establecimientoId;
    private String establecimientoNombre;
    private String establecimientoDireccion;
    private LocalDateTime fechaPresentacion;
    private EstadoExpediente estadoExpediente;
    private EstadoValidacionPago estadoPagoSat;
    private ResultadoZonificacion resultadoZonificacion;
    private NivelRiesgo nivelRiesgoDeclarado;
    private boolean sustentoItsePresente;
    private long observacionesPendientes;
    private String responsableAsignado;
    private LocalDate fechaLimite;
    private String alertaPlazo;
    private UUID informeId;
    private EstadoInforme informeEstado;
    private ConclusionInforme informeConclusion;
}
