package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO acotado para el seguimiento público del ciudadano (H07 / Ley N° 29733).
 * Protege la privacidad del administrado omitiendo datos personales sensibles
 * (teléfono, correo, facultades SUNARP, Anexo 4) y ofuscando el documento de identidad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoCiudadanoDto {

    private UUID id;
    private String numeroTramite;
    private EstadoExpediente estado;
    private ModalidadTramite modalidadTramite;

    // Identificación ofuscada para proteger la identidad del titular
    private String titularOfuscado;
    private String documentoIdentidadOfuscado;
    private String razonSocial;
    private String nombreComercial;
    private String giroNegocio;
    private String direccionEstablecimiento;
    private BigDecimal areaMetrosCuadrados;
    private String tipoItse;

    // Estado del trámite y plazos legales (Ley N° 28976)
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLimite;
    private Long diasHabilesRestantes;
    private Boolean alertaVencimiento;

    // Liquidación tributaria y resultado
    private NivelRiesgo nivelRiesgo;
    private BigDecimal montoTasa;
    private String voucherId;
    private String licenciaQrCode;

    // Observaciones y Subsanaciones (Ley N° 27444 LPAG)
    private String motivoObservacion;
    private LocalDateTime fechaObservacion;
    private LocalDateTime fechaSubsanacion;
    private String detalleSubsanacion;
}
