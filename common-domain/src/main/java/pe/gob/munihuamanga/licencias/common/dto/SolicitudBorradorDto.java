package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudBorradorDto {

    private UUID id;
    private UUID cuentaId;
    private UUID establecimientoId;
    private String nombreEstablecimiento;
    private String direccionEstablecimiento;
    private UUID creadorId;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private EstadoBorrador estado;
    private Long version;

    private ModalidadTramite modalidadTramite;
    private String nombreComercialDeclarado;
    private String giroNegocioDeclarado;
    private String actividadDetallada;
    private BigDecimal areaMetrosCuadrados;
    private Integer aforoPersonas;
    private Integer numeroPisos;
    private String horarioInicio;
    private String horarioFin;
    private String telefonoContacto;
    private String correoNotificacion;
    private Boolean autorizaNotificacion;
    private NivelRiesgo nivelRiesgoDeclarado;

    // Datos del comprobante SAT registrado
    private String numeroOperacionSat;
    private LocalDate fechaPagoSat;
    private BigDecimal montoTasa;
    private String voucherId;

    private Anexo4CondicionesDto anexo4Condiciones;
    private List<DocumentoBorradorDto> documentos;

    private UUID expedienteId;
    private String numeroTramiteFormal;

    // Advertencias legales y transparencia institucional
    private String avisoLegal;
    private String avisoZonificacion;
    private String avisoPagoSat;
    private String avisoRiesgoItse;
}
