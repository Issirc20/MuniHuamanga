package pe.gob.munihuamanga.licencias.expedientes.evaluacion.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA para el Informe Administrativo Técnico de Licencias de Funcionamiento (M08 - I07).
 */
@Entity
@Table(name = "informes_administrativos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformeAdministrativo {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(name = "numero_informe", nullable = false, length = 50)
    private String numeroInforme;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "evaluador_id", nullable = false)
    private UUID evaluadorId;

    @Column(name = "evaluador_nombre", nullable = false, length = 200)
    private String evaluadorNombre;

    @Column(name = "revisor_id")
    private UUID revisorId;

    @Column(name = "revisor_nombre", length = 200)
    private String revisorNombre;

    @Column(name = "aprobador_id")
    private UUID aprobadorId;

    @Column(name = "aprobador_nombre", length = 200)
    private String aprobadorNombre;

    @Column(name = "fecha_elaboracion", nullable = false)
    private LocalDateTime fechaElaboracion;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "conclusion", nullable = false, length = 30)
    private ConclusionInforme conclusion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoInforme estado;

    @Column(name = "fundamentos_administrativos", nullable = false, columnDefinition = "TEXT")
    private String fundamentosAdministrativos;

    @Column(name = "referencias_documentales", nullable = false, columnDefinition = "TEXT")
    private String referenciasDocumentales;

    @Column(name = "resultados_checklist_json", columnDefinition = "TEXT")
    private String resultadosChecklistJson;

    @Column(name = "observaciones_revision", columnDefinition = "TEXT")
    private String observacionesRevision;

    @Column(name = "es_version_actual", nullable = false)
    @Builder.Default
    private Boolean esVersionActual = true;

    @Column(name = "version_anterior_id")
    private UUID versionAnteriorId;

    @OneToMany(mappedBy = "informe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("fecha ASC")
    @Builder.Default
    private List<HistorialInformeAdministrativo> historial = new ArrayList<>();
}
