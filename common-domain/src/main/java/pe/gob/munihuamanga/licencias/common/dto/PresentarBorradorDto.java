package pe.gob.munihuamanga.licencias.common.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentarBorradorDto {

    @NotNull(message = "Debe declarar la aceptación de la Declaración Jurada")
    @AssertTrue(message = "La Declaración Jurada debe ser aceptada expresamente para formalizar la presentación")
    private Boolean declaracionJuradaAceptada;

    @NotNull(message = "Debe confirmar la presentación formal de la solicitud")
    @AssertTrue(message = "Debe confirmar expresamente el envío formal para generar el número de expediente")
    private Boolean confirmacionPresentacion;

    private String comentarios;
}
