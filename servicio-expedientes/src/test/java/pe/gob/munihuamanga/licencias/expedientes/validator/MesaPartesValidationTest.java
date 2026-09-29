package pe.gob.munihuamanga.licencias.expedientes.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.gob.munihuamanga.licencias.common.dto.CrearExpedienteDto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesaPartesValidationTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Debe aceptar DNI peruano válido de 8 dígitos numéricos")
    void testDniValido() {
        CrearExpedienteDto dto = crearDtoBase();
        dto.setDocumentoIdentidad("45879632");

        Set<ConstraintViolation<CrearExpedienteDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Un DNI de 8 dígitos debe ser válido");
    }

    @Test
    @DisplayName("Debe aceptar RUC peruano válido de 11 dígitos iniciado con 10 o 20")
    void testRucValido() {
        CrearExpedienteDto dto10 = crearDtoBase();
        dto10.setDocumentoIdentidad("10458796321");
        assertTrue(validator.validate(dto10).isEmpty(), "RUC iniciado con 10 debe ser válido");

        CrearExpedienteDto dto20 = crearDtoBase();
        dto20.setDocumentoIdentidad("20601234567");
        assertTrue(validator.validate(dto20).isEmpty(), "RUC iniciado con 20 debe ser válido");
    }

    @Test
    @DisplayName("Debe rechazar DNI con longitud menor a 8 o caracteres no numéricos")
    void testDniInvalido() {
        CrearExpedienteDto dtoCorto = crearDtoBase();
        dtoCorto.setDocumentoIdentidad("45879");
        assertFalse(validator.validate(dtoCorto).isEmpty());

        CrearExpedienteDto dtoLetras = crearDtoBase();
        dtoLetras.setDocumentoIdentidad("4587ABCD");
        assertFalse(validator.validate(dtoLetras).isEmpty());
    }

    @Test
    @DisplayName("Debe rechazar RUC con inicio no permitido o longitud diferente a 11")
    void testRucInvalido() {
        CrearExpedienteDto dtoRucInvalido = crearDtoBase();
        dtoRucInvalido.setDocumentoIdentidad("15601234567"); // No inicia con 10 ni 20
        assertFalse(validator.validate(dtoRucInvalido).isEmpty());
    }

    @Test
    @DisplayName("Debe rechazar área menor a 1.0 m²")
    void testAreaInvalida() {
        CrearExpedienteDto dtoAreaCero = crearDtoBase();
        dtoAreaCero.setAreaMetrosCuadrados(new BigDecimal("0.50"));
        assertFalse(validator.validate(dtoAreaCero).isEmpty());
    }

    @Test
    @DisplayName("Debe validar solicitud de Persona Jurídica con RUC 20, SUNARP y Anexo 4")
    void testPersonaJuridicaValida() {
        CrearExpedienteDto dto = crearDtoBase();
        dto.setTipoPersona(pe.gob.munihuamanga.licencias.common.enums.TipoPersona.JURIDICA);
        dto.setTipoDocumento(pe.gob.munihuamanga.licencias.common.enums.TipoDocumento.RUC);
        dto.setDocumentoIdentidad("20601234567");
        dto.setRazonSocial("INVERSIONES LOS RETABLOS S.A.C.");
        dto.setPartidaSunarp("11029384");
        dto.setAsientoSunarp("A0001");
        dto.setDniRepresentante("42567891");
        dto.setNombreRepresentante("María Quispe Huamán");
        dto.setModalidadTramite(pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite.LICENCIA_INDETERMINADA);
        dto.setFuncionEdificacion(pe.gob.munihuamanga.licencias.common.enums.FuncionEdificacion.COMERCIO);
        dto.setTipoVia("Jr.");
        dto.setNombreVia("9 de Diciembre");
        dto.setNumeroVivienda("142");
        dto.setUrbanizacion("Centro Histórico");
        dto.setAforoPersonas(20);
        dto.setAnexo4Condiciones(pe.gob.munihuamanga.licencias.common.dto.Anexo4CondicionesDto.builder()
                .areaTerreno(new BigDecimal("50.00"))
                .aforoPersonas(20)
                .extintoresOperativos(true)
                .pozoTierraVigente(true)
                .build());

        Set<ConstraintViolation<CrearExpedienteDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "La solicitud de persona jurídica con Anexo 4 debe ser válida");
    }

    private CrearExpedienteDto crearDtoBase() {
        return CrearExpedienteDto.builder()
                .solicitanteId(UUID.randomUUID())
                .nombreTitular("Carlos Huamán Rivera")
                .documentoIdentidad("70123456")
                .nombreComercial("Cafetería Los Portales")
                .giroNegocio("Venta de café y postres")
                .direccionEstablecimiento("Jr. Callao 120, Ayacucho")
                .areaMetrosCuadrados(new BigDecimal("40.00"))
                .correoElectronico("carlos.huaman@gmail.com")
                .telefono("966123456")
                .build();
    }
}
