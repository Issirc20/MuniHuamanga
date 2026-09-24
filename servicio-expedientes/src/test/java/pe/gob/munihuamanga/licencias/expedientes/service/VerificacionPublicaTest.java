package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionLicenciaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de Verificación Pública de Licencias RNF-20 (Sprint 3)")
class VerificacionPublicaTest {

    @Mock
    private ExpedienteRepository expedienteRepository;

    @InjectMocks
    private ExpedienteService expedienteService;

    @Test
    @DisplayName("US-11: Debe retornar estado VIGENTE y valida=true para un código de licencia aprobado")
    void testVerificarLicenciaAprobada() {
        String codigoLicencia = "LIC-2026-X9Y8Z7W6";
        Expediente exp = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00001")
                .licenciaQrCode(codigoLicencia)
                .estado(EstadoExpediente.APROBADO)
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad("45879632")
                .razonSocial("COMERCIAL HUAMANGA S.A.C.")
                .nombreComercial("Boutique Tradición")
                .giroNegocio("Venta de prendas y artesanías")
                .direccionEstablecimiento("Jr. 28 de Julio N° 120, Huamanga")
                .nivelRiesgo(NivelRiesgo.BAJO)
                .montoTasa(new BigDecimal("154.50"))
                .fechaCreacion(LocalDateTime.now().minusDays(2))
                .build();

        when(expedienteRepository.findByLicenciaQrCode(codigoLicencia)).thenReturn(Optional.of(exp));

        VerificacionLicenciaDto resultado = expedienteService.verificarLicencia(codigoLicencia);

        assertNotNull(resultado);
        assertTrue(resultado.isValida(), "La licencia debe ser válida");
        assertEquals(codigoLicencia, resultado.getNumeroLicencia());
        assertEquals("VIGENTE / AUTORIZADA", resultado.getEstado());
        assertEquals("María Quispe Huamán", resultado.getTitular());
        assertEquals("Jr. 28 de Julio N° 120, Huamanga", resultado.getDireccion());
        assertTrue(resultado.getMensajeVerificacion().contains("auténtica y vigente"));
    }

    @Test
    @DisplayName("US-11: Debe retornar valida=false si el trámite no está en estado APROBADO")
    void testVerificarLicenciaNoAprobada() {
        String codigoLicencia = "LIC-2026-PENDIENTE";
        Expediente exp = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00002")
                .licenciaQrCode(codigoLicencia)
                .estado(EstadoExpediente.EN_EVALUACION_FINAL)
                .nombreTitular("Juan Perez")
                .build();

        when(expedienteRepository.findByLicenciaQrCode(codigoLicencia)).thenReturn(Optional.of(exp));

        VerificacionLicenciaDto resultado = expedienteService.verificarLicencia(codigoLicencia);

        assertNotNull(resultado);
        assertFalse(resultado.isValida(), "No debe ser válida si no está en estado APROBADO");
        assertTrue(resultado.getEstado().contains("NO VIGENTE"));
    }

    @Test
    @DisplayName("US-11: Debe retornar NO ENCONTRADA y valida=false si el código no existe")
    void testVerificarLicenciaInexistente() {
        String codigoInexistente = "LIC-9999-INEXISTENTE";
        when(expedienteRepository.findByLicenciaQrCode(codigoInexistente)).thenReturn(Optional.empty());

        VerificacionLicenciaDto resultado = expedienteService.verificarLicencia(codigoInexistente);

        assertNotNull(resultado);
        assertFalse(resultado.isValida());
        assertEquals("NO ENCONTRADA", resultado.getEstado());
    }

    @Test
    @DisplayName("US-11: Debe validar adecuadamente códigos vacíos o nulos")
    void testVerificarLicenciaNula() {
        VerificacionLicenciaDto res1 = expedienteService.verificarLicencia(null);
        assertFalse(res1.isValida());
        assertEquals("CÓDIGO INVÁLIDO", res1.getEstado());

        VerificacionLicenciaDto res2 = expedienteService.verificarLicencia("   ");
        assertFalse(res2.isValida());
        assertEquals("CÓDIGO INVÁLIDO", res2.getEstado());
    }
}
