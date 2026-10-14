package pe.gob.munihuamanga.licencias.expedientes.licencias.model;

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
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA LicenciaEmitida (M09 - Incremento I08).
 * Documento oficial inmutable expedido por la Subgerencia de Comercio, Licencias y Control Sanitario.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "licencias_emitidas", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(name = "uk_licencia_expediente", columnNames = {"expediente_id"}),
        @jakarta.persistence.UniqueConstraint(name = "uk_licencia_resolucion", columnNames = {"resolucion_id"})
})
public class LicenciaEmitida {

    @Id
    private UUID id;

    @Column(name = "numero_licencia", nullable = false, unique = true, length = 30)
    private String numeroLicencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolucion_id", nullable = false)
    private ResolucionAdministrativa resolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establecimiento_id")
    private Establecimiento establecimiento;

    @Column(name = "codigo_verificacion_qr", nullable = false, unique = true, length = 64)
    private String codigoVerificacionQr;

    @Column(name = "titular_tipo_documento", nullable = false, length = 20)
    private String titularTipoDocumento;

    @Column(name = "titular_numero_documento", nullable = false, length = 20)
    private String titularNumeroDocumento;

    @Column(name = "titular_nombre_o_razon_social", nullable = false, length = 200)
    private String titularNombreORazonSocial;

    @Column(name = "nombre_comercial", nullable = false, length = 200)
    private String nombreComercial;

    @Column(name = "direccion_establecimiento", nullable = false, columnDefinition = "TEXT")
    private String direccionEstablecimiento;

    @Column(name = "giro_actividad", nullable = false, columnDefinition = "TEXT")
    private String giroActividad;

    @Column(name = "ciiu_codigo", length = 10)
    private String ciiuCodigo;

    @Column(name = "area_metros_cuadrados", precision = 10, scale = 2)
    private BigDecimal areaMetrosCuadrados;

    @Column(name = "aforo_personas")
    private Integer aforoPersonas;

    @Column(name = "zonificacion", length = 50)
    private String zonificacion;

    @Column(name = "nivel_riesgo_itse", length = 30)
    private String nivelRiesgoItse;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vigencia", nullable = false, length = 20)
    private TipoVigenciaLicencia tipoVigencia;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_caducidad")
    private LocalDate fechaCaducidad;

    @Column(name = "autoridad_emisora", nullable = false, length = 150)
    private String autoridadEmisora;

    @Column(name = "hash_pdf", nullable = false, length = 64)
    private String hashPdf;

    @Column(name = "archivo_pdf", columnDefinition = "BYTEA")
    private byte[] archivoPdf;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoLicencia estado;

    @Column(name = "observaciones_emision", columnDefinition = "TEXT")
    private String observacionesEmision;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "creado_por", nullable = false, length = 100)
    private String creadoPor;
}
