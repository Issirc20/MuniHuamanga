package pe.gob.munihuamanga.licencias.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de Consulta Móvil para Inspectores Municipales (Incremento I09 - M10).
 * Contiene datos autorizados para la función de fiscalización en campo (identificación,
 * georreferenciación oficial, aforo, área y condiciones técnicas de la licencia).
 * Acceso restringido a ROLE_INSPECTOR y ROLE_ADMIN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificacionInspectorDto {

    private String codigoVerificacionQr;
    private String numeroLicencia;
    private String numeroExpediente;
    private String numeroResolucion;
    private String nombreComercial;
    private String titularNombreORazonSocial;
    private String titularTipoDocumento;
    private String titularNumeroDocumento;
    private String direccionAutorizada;
    private String referenciaUbicacion;

    // Coordenadas geográficas y geolocalización
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String tipoCoordenada; // "DECLARADA_OFICIAL" o "RECTIFICADA_APROBADA"
    private boolean coordenadasVerificadas;
    private String enlaceNavegacion;

    // Parámetros técnicos del establecimiento
    private String giroActividad;
    private String ciiuCodigo;
    private BigDecimal areaMetrosCuadrados;
    private Integer aforoPersonas;
    private String zonificacion;
    private String nivelRiesgoItse;

    // Vigencia y validez
    private TipoVigenciaLicencia tipoVigencia;
    private LocalDate fechaEmision;
    private LocalDate fechaCaducidad;
    private String autoridadEmisora;
    private EstadoLicencia estadoLicencia;
    private boolean firmaDigitalValida;
    private boolean valida;
    private boolean esSimulada;
    private String mensajeVerificacion;

    // Trazabilidad de inspección
    private String inspectorConsultante;
    private LocalDateTime fechaConsulta;
}
