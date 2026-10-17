package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;

/**
 * DTO que encapsula los datos del Anexo 1 ITSE:
 * Solicitud de Inspección Técnica de Seguridad en Edificaciones (ITSE) y ECSE (D.S. N° 002-2018-PCM).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anexo1ItseDto {

    // I. Información General
    private String numeroExpediente;
    @Builder.Default
    private String tipoItse = "POSTERIOR"; // POSTERIOR, PREVIA, RENOVACION
    @Builder.Default
    private String tipoEcse = "NINGUNO";   // NINGUNO, HASTA_3000, MAYOR_3000
    @Builder.Default
    private String funcionEdificacion = "COMERCIO"; // COMERCIO, ENCUENTRO, HOSPEDAJE, EDUCACION, INDUSTRIAL, OFICINAS, SALUD, ALMACEN
    private NivelRiesgo nivelRiesgo; // Calculado preliminarmente o dictaminado por evaluador

    // II. Datos del Solicitante
    @Builder.Default
    private String condicionSolicitante = "CONDUCTOR_ADMINISTRADOR"; // PROPIETARIO, REPRESENTANTE_LEGAL, CONDUCTOR_ADMINISTRADOR, ORGANIZADOR_PROMOTOR
    private String nombresSolicitante;
    private String dniCeSolicitante;
    private String domicilioSolicitante;
    private String telefonoSolicitante;
    private String correoSolicitante;

    // III. Datos del Establecimiento Objeto de Inspección
    private String razonSocial;
    private String ruc;
    private String nombreComercial;
    private String direccionEstablecimiento;
    @Builder.Default
    private String localidad = "Ayacucho";
    @Builder.Default
    private String distrito = "Ayacucho";
    @Builder.Default
    private String provincia = "Huamanga";
    @Builder.Default
    private String departamento = "Ayacucho";
    private String referenciaDireccion;
    private String giroActividades;
    private Integer numeroPisos;
    @Builder.Default
    private String pisoUbicacion = "1er Piso";
    private BigDecimal areaOcupadaTotal;
    @Builder.Default
    private String horarioAtencion = "08:00 - 20:00";

    // IV. Documentos Presentados (Checkboxes según D.S. N° 002-2018-PCM)
    @Builder.Default
    private Boolean docDeclaracionJurada = true;
    @Builder.Default
    private Boolean docReciboPago = true;
    @Builder.Default
    private Boolean docCroquisUbicacion = true;
    @Builder.Default
    private Boolean docPlanoArquitecturaAforo = false;
    @Builder.Default
    private Boolean docPlanoTableros = false;
    @Builder.Default
    private Boolean docPlanSeguridad = false;
    @Builder.Default
    private Boolean docPruebasOperatividad = false;
    @Builder.Default
    private Boolean docCertificadoPozoTierra = false;
    @Builder.Default
    private Boolean docConformidadObra = false;
    private String resolucionConformidadObra;
}
