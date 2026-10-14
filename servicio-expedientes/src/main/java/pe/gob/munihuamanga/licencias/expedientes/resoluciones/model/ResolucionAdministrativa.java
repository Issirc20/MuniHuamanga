package pe.gob.munihuamanga.licencias.expedientes.resoluciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA ResolucionAdministrativa (M09 - Incremento I08).
 * Acto resolutivo formal emitido por la autoridad competente de la Municipalidad Provincial de Huamanga.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "resoluciones_administrativas")
public class ResolucionAdministrativa {

    @Id
    private UUID id;

    @Column(name = "numero_resolucion", nullable = false, unique = true, length = 30)
    private String numeroResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "informe_administrativo_id", nullable = false)
    private InformeAdministrativo informeAdministrativo;

    @Column(name = "version_informe", nullable = false)
    private Integer versionInforme;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_decision", nullable = false, length = 20)
    private TipoDecisionResolucion tipoDecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoResolucion estado;

    @Column(name = "fundamentos", nullable = false, columnDefinition = "TEXT")
    private String fundamentos;

    @Column(name = "referencias_legales", columnDefinition = "TEXT")
    private String referenciasLegales;

    @Column(name = "parte_resolutiva", columnDefinition = "TEXT")
    private String parteResolutiva;

    @Column(name = "autoridad_competente_id")
    private UUID autoridadCompetenteId;

    @Column(name = "autoridad_competente_nombre", length = 150)
    private String autoridadCompetenteNombre;

    @Column(name = "autoridad_competente_cargo", length = 150)
    private String autoridadCompetenteCargo;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Column(name = "firmante_nombre", length = 150)
    private String firmanteNombre;

    @Column(name = "firmante_cargo", length = 150)
    private String firmanteCargo;

    @Column(name = "firmante_entidad", length = 150)
    private String firmanteEntidad;

    @Column(name = "hash_documento", length = 64)
    private String hashDocumento;

    @Column(name = "archivo_pdf")
    private byte[] archivoPdf;

    @Column(name = "observaciones_firma", columnDefinition = "TEXT")
    private String observacionesFirma;

    @Column(name = "es_version_actual", nullable = false)
    private Boolean esVersionActual;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "creado_por", nullable = false, length = 100)
    private String creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "modificado_por", length = 100)
    private String modificadoPor;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;
}
