package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Día no laborable del calendario institucional (feriado nacional, regional o día no laborable decretado).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaNoLaborableDto {

    @NotNull(message = "La fecha es obligatoria.")
    private LocalDate fecha;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 150, message = "La descripción no debe exceder los 150 caracteres.")
    private String descripcion;

    @Size(max = 200, message = "La base legal no debe exceder los 200 caracteres.")
    private String baseLegal;
}
