package pe.gob.munihuamanga.licencias.expedientes.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.exception.TransicionInvalidaException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstadoExpedienteValidatorTest {

    private EstadoExpedienteValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EstadoExpedienteValidator();
    }

    @Test
    @DisplayName("Debe permitir transición de FORMATOS_GENERADOS a DOCUMENTOS_VALIDADOS")
    void transicionValidaFormatosAValidados() {
        assertDoesNotThrow(() ->
                validator.validarTransicion(EstadoExpediente.FORMATOS_GENERADOS, EstadoExpediente.DOCUMENTOS_VALIDADOS)
        );
    }

    @Test
    @DisplayName("Debe permitir transición de DOCUMENTOS_VALIDADOS a EN_EVALUACION_FINAL")
    void transicionValidaValidadosAEvaluacionFinal() {
        assertDoesNotThrow(() ->
                validator.validarTransicion(EstadoExpediente.DOCUMENTOS_VALIDADOS, EstadoExpediente.EN_EVALUACION_FINAL)
        );
    }

    @Test
    @DisplayName("Debe permitir transición de EN_EVALUACION_FINAL a APROBADO")
    void transicionValidaEvaluacionFinalAAprobado() {
        assertDoesNotThrow(() ->
                validator.validarTransicion(EstadoExpediente.EN_EVALUACION_FINAL, EstadoExpediente.APROBADO)
        );
    }

    @Test
    @DisplayName("Debe permitir transición de EN_EVALUACION_FINAL a RECHAZADO")
    void transicionValidaEvaluacionFinalARechazado() {
        assertDoesNotThrow(() ->
                validator.validarTransicion(EstadoExpediente.EN_EVALUACION_FINAL, EstadoExpediente.RECHAZADO)
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar saltar de FORMATOS_GENERADOS directo a APROBADO")
    void transicionInvalidaSaltoDirecto() {
        assertThrows(TransicionInvalidaException.class, () ->
                validator.validarTransicion(EstadoExpediente.FORMATOS_GENERADOS, EstadoExpediente.APROBADO)
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar cambiar de estado desde un estado terminal (APROBADO)")
    void transicionInvalidaDesdeEstadoTerminal() {
        assertThrows(TransicionInvalidaException.class, () ->
                validator.validarTransicion(EstadoExpediente.APROBADO, EstadoExpediente.EN_EVALUACION_FINAL)
        );
    }
}
