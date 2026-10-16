package pe.gob.munihuamanga.licencias.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de Verificación Pública con Minimización Estricta de Datos (Ley N° 29733 / ERS RNF-SEG-007 / Incremento I09).
 * No expone DNI, IDs internos de base de datos, teléfonos, correos ni datos sensibles del titular.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificacionPublicaQrDto {

    private String codigoVerificacionQr;
    private String numeroLicencia;
    private String numeroResolucion;
    private String nombreComercial;
    private String titularNombreORazonSocial;
    private String titularDocumentoEnmascarado;
    private String direccionEstablecimiento;
    private String giroActividad;
    private String ciiuCodigo;
    private TipoVigenciaLicencia tipoVigencia;
    private LocalDate fechaEmision;
    private LocalDate fechaCaducidad;
    private String autoridadEmisora;
    private EstadoLicencia estadoLicencia;
    private boolean firmaDigitalValida;
    private boolean valida;
    private boolean esSimulada;
    private String mensajeVerificacion;
    private LocalDateTime fechaConsulta;
}
