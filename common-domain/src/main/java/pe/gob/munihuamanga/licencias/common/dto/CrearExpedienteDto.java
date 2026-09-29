package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.FuncionEdificacion;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para el registro de solicitudes en Mesa de Partes Virtual
 * Cumple con todos los campos estandarizados del Anexo 1 (Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 163-2020-PCM).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearExpedienteDto {

    @NotNull(message = "El identificador del solicitante es obligatorio")
    private UUID solicitanteId;

    // Sección I: Modalidad del Trámite
    @Builder.Default
    private ModalidadTramite modalidadTramite = ModalidadTramite.LICENCIA_INDETERMINADA;
    private Integer plazoTemporalMeses;
    private String tipoAnuncio;
    private String numeroLicenciaPrincipal;

    // Sección II: Datos del Solicitante
    @Builder.Default
    private TipoPersona tipoPersona = TipoPersona.NATURAL;

    @Builder.Default
    private TipoDocumento tipoDocumento = TipoDocumento.DNI;

    @NotBlank(message = "El nombre del titular o representante es obligatorio")
    private String nombreTitular;

    @NotBlank(message = "El documento de identidad es obligatorio")
    @Pattern(
            regexp = "^([0-9]{8}|(10|20)[0-9]{9})$",
            message = "El documento debe ser un DNI válido de 8 dígitos o un RUC válido de 11 dígitos iniciado con 10 o 20"
    )
    private String documentoIdentidad;

    private String razonSocial;

    @Pattern(regexp = "^(9[0-9]{8})?$", message = "El teléfono debe ser un número celular peruano de 9 dígitos")
    private String telefono;

    @Email(message = "El formato del correo electrónico de notificación no es válido")
    private String correoElectronico;

    @Builder.Default
    private Boolean autorizaNotificacion = true;

    // Sección III: Representante Legal o Apoderado (SUNARP)
    private String partidaSunarp;
    private String asientoSunarp;
    private String dniRepresentante;
    private String nombreRepresentante;
    private String poderSunarp;

    // Sección IV: Datos del Establecimiento
    @NotBlank(message = "El nombre comercial es obligatorio")
    private String nombreComercial;

    private String ciiuCodigo;

    @NotBlank(message = "El giro del negocio es obligatorio")
    private String giroNegocio;

    private String actividadDetallada;

    private String zonificacion;

    @Builder.Default
    private FuncionEdificacion funcionEdificacion = FuncionEdificacion.COMERCIO;

    // Dirección desglosada del Anexo 1
    @NotBlank(message = "La dirección del establecimiento es obligatoria")
    private String direccionEstablecimiento;

    private String tipoVia;            // Av., Jr., Ca., Pje., etc.
    private String nombreVia;
    private String numeroVivienda;
    private String interior;
    private String manzana;
    private String lote;
    private String urbanizacion;
    @Builder.Default
    private String distrito = "Ayacucho";
    @Builder.Default
    private String provincia = "Huamanga";
    @Builder.Default
    private String departamento = "Ayacucho";
    private String referenciaUbicacion;

    // Dimensionamiento
    @NotNull(message = "El área en metros cuadrados es obligatoria")
    @DecimalMin(value = "1.0", message = "El área del establecimiento debe ser de al menos 1.0 m²")
    private BigDecimal areaMetrosCuadrados;

    private BigDecimal areaTerreno;
    private BigDecimal areaTechadaTotal;
    private BigDecimal areaOcupadaTotal;
    private Integer aforoPersonas;
    private Integer numeroPisos;
    private Integer antiguedadEdificacion;
    private Integer antiguedadGiro;

    // Autorización Sectorial
    @Builder.Default
    private Boolean requiereAutorizacionSectorial = false;
    private String sectorEntidad;
    private String sectorDenominacion;
    private String sectorFecha;
    private String sectorNumero;

    // Anexo 4 (Condiciones de Seguridad en Edificación para Riesgo Bajo/Medio)
    private Anexo4CondicionesDto anexo4Condiciones;
}
