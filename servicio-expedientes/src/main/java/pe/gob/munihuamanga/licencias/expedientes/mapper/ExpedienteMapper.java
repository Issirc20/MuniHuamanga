package pe.gob.munihuamanga.licencias.expedientes.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import pe.gob.munihuamanga.licencias.common.dto.Anexo4CondicionesDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialEstadoDto;
import pe.gob.munihuamanga.licencias.common.dto.SeguimientoCiudadanoDto;
import pe.gob.munihuamanga.licencias.expedientes.model.Anexo4Condiciones;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.model.HistorialEstado;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CalculadoraPlazos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Conversión entre la entidad Expediente y los DTO expuestos.
 * Los días hábiles restantes se calculan con el calendario institucional (incluye feriados).
 */
@Mapper(componentModel = "spring")
public abstract class ExpedienteMapper {

    private static final long DIAS_ALERTA_VENCIMIENTO = 3;

    @Autowired
    protected CalculadoraPlazos calculadoraPlazos;

    @Mapping(target = "diasHabilesRestantes", source = "fechaLimite", qualifiedByName = "calcularDiasHabiles")
    @Mapping(target = "alertaVencimiento", source = "fechaLimite", qualifiedByName = "evaluarAlertaVencimiento")
    @Mapping(target = "tipoItse", expression = "java(expediente.getTipoItse())")
    public abstract ExpedienteResponseDto toDto(Expediente expediente);

    @Mapping(target = "diasHabilesRestantes", source = "fechaLimite", qualifiedByName = "calcularDiasHabiles")
    @Mapping(target = "alertaVencimiento", source = "fechaLimite", qualifiedByName = "evaluarAlertaVencimiento")
    @Mapping(target = "titularOfuscado", source = "nombreTitular", qualifiedByName = "ofuscarNombre")
    @Mapping(target = "documentoIdentidadOfuscado", source = "documentoIdentidad", qualifiedByName = "ofuscarDocumento")
    @Mapping(target = "tipoItse", expression = "java(expediente.getTipoItse())")
    @Mapping(target = "motivoObservacion", ignore = true)
    @Mapping(target = "detalleSubsanacion", ignore = true)
    @Mapping(target = "voucherId", ignore = true)
    @Mapping(target = "licenciaQrCode", ignore = true)
    public abstract SeguimientoCiudadanoDto toSeguimientoDto(Expediente expediente);

    public abstract List<ExpedienteResponseDto> toDtoList(List<Expediente> expedientes);

    public abstract HistorialEstadoDto toHistorialDto(HistorialEstado historial);

    public abstract List<HistorialEstadoDto> toHistorialDtoList(List<HistorialEstado> historiales);

    public abstract Anexo4CondicionesDto toAnexo4Dto(Anexo4Condiciones entity);

    public abstract Anexo4Condiciones toAnexo4Entity(Anexo4CondicionesDto dto);

    /** Días hábiles restantes del plazo legal; es nulo mientras el plazo no corre. */
    @Named("calcularDiasHabiles")
    protected Long calcularDiasHabiles(LocalDateTime fechaLimite) {
        if (fechaLimite == null || calculadoraPlazos == null) {
            return null;
        }
        return (long) calculadoraPlazos.contarDiasHabiles(LocalDate.now(), fechaLimite.toLocalDate());
    }

    @Named("evaluarAlertaVencimiento")
    protected Boolean evaluarAlertaVencimiento(LocalDateTime fechaLimite) {
        Long dias = calcularDiasHabiles(fechaLimite);
        return dias != null && dias <= DIAS_ALERTA_VENCIMIENTO;
    }

    @Named("ofuscarNombre")
    protected String ofuscarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String parte : nombre.trim().split("\\s+")) {
            if (!parte.isEmpty()) {
                sb.append(parte.charAt(0)).append("*** ");
            }
        }
        return sb.toString().trim();
    }

    @Named("ofuscarDocumento")
    protected String ofuscarDocumento(String doc) {
        if (doc == null || doc.length() < 4) {
            return "****";
        }
        return doc.substring(0, 2) + "***" + doc.substring(doc.length() - 2);
    }
}
