package pe.gob.munihuamanga.licencias.expedientes.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pe.gob.munihuamanga.licencias.common.dto.Anexo4CondicionesDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialEstadoDto;
import pe.gob.munihuamanga.licencias.expedientes.model.Anexo4Condiciones;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.model.HistorialEstado;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper MapStruct para transformar entre entidades de base de datos y DTOs expuestos.
 */
@Mapper(componentModel = "spring")
public interface ExpedienteMapper {

    @Mapping(target = "diasHabilesRestantes", source = "fechaLimite", qualifiedByName = "calcularDiasHabiles")
    @Mapping(target = "alertaVencimiento", source = "fechaLimite", qualifiedByName = "evaluarAlertaVencimiento")
    @Mapping(target = "tipoItse", expression = "java(expediente.getTipoItse())")
    ExpedienteResponseDto toDto(Expediente expediente);

    @Mapping(target = "diasHabilesRestantes", source = "fechaLimite", qualifiedByName = "calcularDiasHabiles")
    @Mapping(target = "alertaVencimiento", source = "fechaLimite", qualifiedByName = "evaluarAlertaVencimiento")
    @Mapping(target = "titularOfuscado", source = "nombreTitular", qualifiedByName = "ofuscarNombre")
    @Mapping(target = "documentoIdentidadOfuscado", source = "documentoIdentidad", qualifiedByName = "ofuscarDocumento")
    @Mapping(target = "tipoItse", expression = "java(expediente.getTipoItse())")
    pe.gob.munihuamanga.licencias.common.dto.SeguimientoCiudadanoDto toSeguimientoDto(Expediente expediente);

    List<ExpedienteResponseDto> toDtoList(List<Expediente> expedientes);

    HistorialEstadoDto toHistorialDto(HistorialEstado historial);

    List<HistorialEstadoDto> toHistorialDtoList(List<HistorialEstado> historiales);

    Anexo4CondicionesDto toAnexo4Dto(Anexo4Condiciones entity);

    Anexo4Condiciones toAnexo4Entity(Anexo4CondicionesDto dto);

    @Named("calcularDiasHabiles")
    default Long calcularDiasHabiles(LocalDateTime fechaLimite) {
        if (fechaLimite == null) return null;
        LocalDate hoy = LocalDate.now();
        LocalDate limite = fechaLimite.toLocalDate();
        if (hoy.isAfter(limite)) {
            return 0L;
        }

        long diasHabiles = 0;
        LocalDate actual = hoy;
        while (!actual.isAfter(limite)) {
            DayOfWeek dow = actual.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                diasHabiles++;
            }
            actual = actual.plusDays(1);
        }
        return diasHabiles;
    }

    @Named("evaluarAlertaVencimiento")
    default Boolean evaluarAlertaVencimiento(LocalDateTime fechaLimite) {
        Long dias = calcularDiasHabiles(fechaLimite);
        if (dias == null) return false;
        // Alerta si quedan 3 o menos días hábiles para el límite legal de 15 días hábiles (RNF-22)
        return dias <= 3;
    }

    @Named("ofuscarNombre")
    default String ofuscarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (!p.isEmpty()) {
                sb.append(p.charAt(0)).append("*** ");
            }
        }
        return sb.toString().trim();
    }

    @Named("ofuscarDocumento")
    default String ofuscarDocumento(String doc) {
        if (doc == null || doc.length() < 4) return "****";
        return doc.substring(0, 2) + "***" + doc.substring(doc.length() - 2);
    }
}

