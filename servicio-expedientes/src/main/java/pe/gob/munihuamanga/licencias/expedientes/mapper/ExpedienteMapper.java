package pe.gob.munihuamanga.licencias.expedientes.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialEstadoDto;
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
    ExpedienteResponseDto toDto(Expediente expediente);

    List<ExpedienteResponseDto> toDtoList(List<Expediente> expedientes);

    HistorialEstadoDto toHistorialDto(HistorialEstado historial);

    List<HistorialEstadoDto> toHistorialDtoList(List<HistorialEstado> historiales);

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
}
