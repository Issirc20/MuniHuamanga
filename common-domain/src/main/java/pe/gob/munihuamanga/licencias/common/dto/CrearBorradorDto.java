package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearBorradorDto {

    @NotNull(message = "La cuenta es obligatoria")
    private UUID cuentaId;

    @NotNull(message = "El establecimiento es obligatorio")
    private UUID establecimientoId;

    @NotNull(message = "La modalidad del trámite es obligatoria")
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

    private Anexo4CondicionesDto anexo4Condiciones;
}
