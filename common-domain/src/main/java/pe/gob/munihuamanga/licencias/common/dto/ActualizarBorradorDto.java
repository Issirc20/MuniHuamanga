package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarBorradorDto {

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

    // Datos del comprobante SAT registrado (sujeto a validación manual)
    private String numeroOperacionSat;
    private LocalDate fechaPagoSat;
    private BigDecimal montoTasa;
    private String voucherId;

    private Anexo4CondicionesDto anexo4Condiciones;
}
