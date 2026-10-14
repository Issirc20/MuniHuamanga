package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenciaEmitidaDto {

    private UUID id;
    private String numeroLicencia;
    private UUID resolucionId;
    private String numeroResolucion;
    private UUID expedienteId;
    private String numeroExpediente;
    private UUID establecimientoId;
    private String codigoVerificacionQr;
    private String urlVerificacion;
    private String titularTipoDocumento;
    private String titularNumeroDocumento;
    private String titularNombreORazonSocial;
    private String nombreComercial;
    private String direccionEstablecimiento;
    private String giroActividad;
    private String ciiuCodigo;
    private BigDecimal areaMetrosCuadrados;
    private Integer aforoPersonas;
    private String zonificacion;
    private String nivelRiesgoItse;
    private TipoVigenciaLicencia tipoVigencia;
    private LocalDate fechaEmision;
    private LocalDate fechaCaducidad;
    private String autoridadEmisora;
    private String hashPdf;
    private EstadoLicencia estadoLicencia;
    private String observacionesEmision;
    private LocalDateTime fechaCreacion;
    private String creadoPor;

    @Builder.Default
    private List<HistorialEmisionLicenciaDto> historial = new ArrayList<>();
}
