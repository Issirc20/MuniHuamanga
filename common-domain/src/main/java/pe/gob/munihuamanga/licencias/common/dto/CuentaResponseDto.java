package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para consultas de Cuenta.
 * Incluye declaración explícita de validación de identidad no oficial si no existe integración RENIEC/SUNAT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaResponseDto {

    private UUID id;
    private TipoPersona tipoPersona;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String razonSocialONombre;
    private String email;
    private String telefono;
    private String direccionFiscal;
    private Boolean emailVerificado;
    private EstadoCuenta estado;
    private Boolean identidadValidadaOficialmente;
    private String avisoIdentidad;
    private Boolean activo;
    private LocalDateTime fechaCreacion;

    @Builder.Default
    private List<OperadorCuentaDto> operadores = new ArrayList<>();

    @Builder.Default
    private List<EstablecimientoDto> establecimientos = new ArrayList<>();
}
