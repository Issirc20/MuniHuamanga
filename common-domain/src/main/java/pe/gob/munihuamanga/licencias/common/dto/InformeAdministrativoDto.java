package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformeAdministrativoDto {
    private UUID id;
    private UUID expedienteId;
    private String numeroExpediente;
    private String numeroInforme;
    private Integer version;
    private UUID evaluadorId;
    private String evaluadorNombre;
    private UUID revisorId;
    private String revisorNombre;
    private UUID aprobadorId;
    private String aprobadorNombre;
    private LocalDateTime fechaElaboracion;
    private LocalDateTime fechaRevision;
    private LocalDateTime fechaAprobacion;
    private ConclusionInforme conclusion;
    private EstadoInforme estado;
    private String fundamentosAdministrativos;
    private String referenciasDocumentales;
    private String resultadosChecklistJson;
    private String observacionesRevision;
    private Boolean esVersionActual;
    private UUID versionAnteriorId;
    private List<HistorialDecisionInformeDto> historial;
}
