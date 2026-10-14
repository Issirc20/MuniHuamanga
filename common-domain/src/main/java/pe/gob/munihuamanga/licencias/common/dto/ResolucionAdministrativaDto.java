package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolucionAdministrativaDto {

    private UUID id;
    private String numeroResolucion;
    private UUID expedienteId;
    private String numeroExpediente;
    private UUID informeAdministrativoId;
    private String numeroInforme;
    private Integer versionInforme;
    private TipoDecisionResolucion tipoDecision;
    private EstadoResolucion estadoResolucion;
    private String fundamentos;
    private String referenciasLegales;
    private String parteResolutiva;
    private UUID autoridadCompetenteId;
    private String autoridadCompetenteNombre;
    private String autoridadCompetenteCargo;
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaFirma;
    private String firmanteNombre;
    private String firmanteCargo;
    private String firmanteEntidad;
    private String hashDocumento;
    private String observacionesFirma;
    private boolean esVersionActual;
    private Integer version;
    private String creadoPor;
    private LocalDateTime fechaCreacion;

    @Builder.Default
    private List<HistorialResolucionDto> historial = new ArrayList<>();
}
