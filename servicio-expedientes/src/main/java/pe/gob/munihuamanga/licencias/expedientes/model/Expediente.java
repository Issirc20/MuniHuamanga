package pe.gob.munihuamanga.licencias.expedientes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.FuncionEdificacion;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal de Expediente según el Diagrama de Clases de la arquitectura
 * y la normativa de la Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 163-2020-PCM.
 */
@Entity
@Table(name = "expedientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expediente {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "numero_tramite", nullable = false, unique = true, length = 30)
    private String numeroTramite;

    @Column(name = "solicitante_id", nullable = false)
    private UUID solicitanteId;

    // Sección I: Modalidad del Trámite
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_tramite", length = 40)
    private ModalidadTramite modalidadTramite;

    @Column(name = "plazo_temporal_meses")
    private Integer plazoTemporalMeses;

    @Column(name = "tipo_anuncio", length = 100)
    private String tipoAnuncio;

    @Column(name = "numero_licencia_principal", length = 50)
    private String numeroLicenciaPrincipal;

    // Sección II: Datos del Solicitante
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_persona", length = 20)
    private TipoPersona tipoPersona;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "nombre_titular", nullable = false, length = 150)
    private String nombreTitular;

    @Column(name = "documento_identidad", nullable = false, length = 20)
    private String documentoIdentidad;

    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(name = "correo_electronico", length = 120)
    private String correoElectronico;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "autoriza_notificacion")
    private Boolean autorizaNotificacion;

    // Sección III: Representante Legal o Apoderado (SUNARP)
    @Column(name = "partida_sunarp", length = 50)
    private String partidaSunarp;

    @Column(name = "asiento_sunarp", length = 50)
    private String asientoSunarp;

    @Column(name = "dni_representante", length = 20)
    private String dniRepresentante;

    @Column(name = "nombre_representante", length = 150)
    private String nombreRepresentante;

    @Column(name = "poder_sunarp", length = 100)
    private String poderSunarp;

    // Sección IV: Datos del Establecimiento
    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "ciiu_codigo", length = 20)
    private String ciiuCodigo;

    @Column(name = "giro_negocio", nullable = false, length = 150)
    private String giroNegocio;

    @Column(name = "actividad_detallada", length = 255)
    private String actividadDetallada;

    @Column(name = "zonificacion", length = 50)
    private String zonificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "funcion_edificacion", length = 40)
    private FuncionEdificacion funcionEdificacion;

    @Column(name = "direccion_establecimiento", nullable = false, length = 255)
    private String direccionEstablecimiento;

    // Dirección desglosada
    @Column(name = "tipo_via", length = 30)
    private String tipoVia;

    @Column(name = "nombre_via", length = 150)
    private String nombreVia;

    @Column(name = "numero_vivienda", length = 30)
    private String numeroVivienda;

    @Column(name = "interior", length = 30)
    private String interior;

    @Column(name = "manzana", length = 30)
    private String manzana;

    @Column(name = "lote", length = 30)
    private String lote;

    @Column(name = "urbanizacion", length = 100)
    private String urbanizacion;

    @Column(name = "distrito", length = 100)
    private String distrito;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "departamento", length = 100)
    private String departamento;

    @Column(name = "referencia_ubicacion", length = 255)
    private String referenciaUbicacion;

    // Dimensionamiento
    @Column(name = "area_metros_cuadrados", precision = 10, scale = 2)
    private BigDecimal areaMetrosCuadrados;

    @Column(name = "area_terreno", precision = 10, scale = 2)
    private BigDecimal areaTerreno;

    @Column(name = "area_techada_total", precision = 10, scale = 2)
    private BigDecimal areaTechadaTotal;

    @Column(name = "area_ocupada_total", precision = 10, scale = 2)
    private BigDecimal areaOcupadaTotal;

    @Column(name = "aforo_personas")
    private Integer aforoPersonas;

    @Column(name = "numero_pisos")
    private Integer numeroPisos;

    @Column(name = "antiguedad_edificacion")
    private Integer antiguedadEdificacion;

    @Column(name = "antiguedad_giro")
    private Integer antiguedadGiro;

    // Autorización Sectorial Previa
    @Column(name = "requiere_autorizacion_sectorial")
    private Boolean requiereAutorizacionSectorial;

    @Column(name = "sector_entidad", length = 150)
    private String sectorEntidad;

    @Column(name = "sector_denominacion", length = 200)
    private String sectorDenominacion;

    @Column(name = "sector_fecha", length = 30)
    private String sectorFecha;

    @Column(name = "sector_numero", length = 50)
    private String sectorNumero;

    // Estados, ITSE y Flujo Transaccional
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoExpediente estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", length = 20)
    private NivelRiesgo nivelRiesgo;

    @Column(name = "monto_tasa", precision = 10, scale = 2)
    private BigDecimal montoTasa;

    @Column(name = "voucher_id", length = 50)
    private String voucherId;

    @Column(name = "licencia_qr_code", length = 255)
    private String licenciaQrCode;

    // Datos para el Certificado Oficial de Licencia de Funcionamiento (Formato físico)
    @Column(name = "numero_licencia", length = 30)
    private String numeroLicencia;  // Número correlativo de licencia (Ej: 202613788)

    @Column(name = "categoria_establecimiento", length = 30)
    private String categoriaEstablecimiento;  // Ej: 1-A (INTERMEDIO), 1-B (BAJO), 2-A (ALTO)

    @Column(name = "hora_inicio", length = 10)
    private String horaInicio;  // Hora inicio autorización operar (Ej: 06:00)

    @Column(name = "hora_fin", length = 10)
    private String horaFin;     // Hora fin autorización operar (Ej: 23:00)

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;  // Fecha de emisión de la Licencia

    // Evidencias Externas / Fase 1
    @Column(name = "numero_informe_itse", length = 50)
    private String numeroInformeItse;

    @Column(name = "fecha_informe_itse")
    private LocalDateTime fechaInformeItse;

    @Column(name = "numero_operacion_sat", length = 50)
    private String numeroOperacionSat;

    @Column(name = "fecha_pago_sat")
    private LocalDateTime fechaPagoSat;

    // Observaciones y Subsanaciones (Ley N° 27444 LPAG / Ley N° 28976)
    @Column(name = "motivo_observacion", length = 500)
    private String motivoObservacion;

    @Column(name = "fecha_observacion")
    private LocalDateTime fechaObservacion;

    @Column(name = "fecha_subsanacion")
    private LocalDateTime fechaSubsanacion;

    @Column(name = "detalle_subsanacion", length = 500)
    private String detalleSubsanacion;

    // Anexo 4 Embebido
    @Embedded
    private Anexo4Condiciones anexo4Condiciones;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    /**
     * Determina si según la Ley N° 28976 y D.S. 002-2018-PCM corresponde ITSE previa o posterior.
     */
    public String getTipoItse() {
        if (nivelRiesgo == null) {
            return "POR_CLASIFICAR";
        }
        return (nivelRiesgo == NivelRiesgo.BAJO || nivelRiesgo == NivelRiesgo.MEDIO)
                ? "ITSE_POSTERIOR"
                : "ITSE_PREVIA";
    }

    /**
     * Actualiza el estado del expediente.
     * @param nuevo nuevo estado asignado.
     */
    public void cambiarEstado(EstadoExpediente nuevo) {
        this.estado = nuevo;
    }
}
