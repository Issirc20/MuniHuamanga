package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
 * DTO de respuesta para la trazabilidad y visualización completa del expediente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteResponseDto {

    private UUID id;
    private String numeroTramite;
    private UUID solicitanteId;

    // Modalidad Anexo 1
    private ModalidadTramite modalidadTramite;
    private Integer plazoTemporalMeses;
    private String tipoAnuncio;
    private String numeroLicenciaPrincipal;

    // Datos del Solicitante
    private TipoPersona tipoPersona;
    private TipoDocumento tipoDocumento;
    private String nombreTitular;
    private String documentoIdentidad;
    private String razonSocial;
    private String correoElectronico;
    private String telefono;
    private Boolean autorizaNotificacion;

    // SUNARP
    private String partidaSunarp;
    private String asientoSunarp;
    private String dniRepresentante;
    private String nombreRepresentante;
    private String poderSunarp;

    // Datos del Establecimiento
    private String nombreComercial;
    private String ciiuCodigo;
    private String giroNegocio;
    private String actividadDetallada;
    private String zonificacion;
    private FuncionEdificacion funcionEdificacion;

    // Dirección desglosada
    private String direccionEstablecimiento;
    private String tipoVia;
    private String nombreVia;
    private String numeroVivienda;
    private String interior;
    private String manzana;
    private String lote;
    private String urbanizacion;
    private String distrito;
    private String provincia;
    private String departamento;
    private String referenciaUbicacion;

    // Áreas y aforo
    private BigDecimal areaMetrosCuadrados;
    private BigDecimal areaTerreno;
    private BigDecimal areaTechadaTotal;
    private BigDecimal areaOcupadaTotal;
    private Integer aforoPersonas;
    private Integer numeroPisos;
    private Integer antiguedadEdificacion;
    private Integer antiguedadGiro;

    // Autorización Sectorial
    private Boolean requiereAutorizacionSectorial;
    private String sectorEntidad;
    private String sectorDenominacion;
    private String sectorFecha;
    private String sectorNumero;

    // Estado y Transiciones
    private EstadoExpediente estado;
    private NivelRiesgo nivelRiesgo;
    private String tipoItse; // "ITSE_POSTERIOR" o "ITSE_PREVIA"
    private BigDecimal montoTasa;
    private String voucherId;
    private String licenciaQrCode;

    // Datos del Certificado Oficial de Licencia (Formato físico)
    private String numeroLicencia;          // Número correlativo de licencia (Ej: 202613788)
    private String categoriaEstablecimiento;// Ej: 1-A (INTERMEDIO)
    private String horaInicio;              // Hora inicio para operar (Ej: 06:00)
    private String horaFin;                 // Hora fin para operar (Ej: 23:00)
    private LocalDateTime fechaAprobacion;  // Fecha de emisión de la Licencia

    // Evidencias Externas / Fase 1
    private String numeroInformeItse;
    private LocalDateTime fechaInformeItse;
    private String numeroOperacionSat;
    private LocalDateTime fechaPagoSat;

    // Anexo 4
    private Anexo4CondicionesDto anexo4Condiciones;

    // Plazos y SLA
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLimite;
    private Long diasHabilesRestantes;
    private Boolean alertaVencimiento;
}
