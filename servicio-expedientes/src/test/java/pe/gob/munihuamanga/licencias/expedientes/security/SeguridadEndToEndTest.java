package pe.gob.munihuamanga.licencias.expedientes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.config.UsuarioDataInitializer;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pe.gob.munihuamanga.licencias.expedientes.soporte.DatosDePrueba.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SeguridadEndToEndTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ExpedienteRepository expedienteRepository;

    private Expediente expediente;

    @BeforeEach
    void setUp() {
        expediente = expedienteRepository.save(Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-E2E-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .solicitanteId(UsuarioDataInitializer.CUENTA_MARIA_QUISPE)
                .cuentaId(UsuarioDataInitializer.CUENTA_MARIA_QUISPE)
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad(CIUDADANA_TITULAR)
                .correoElectronico("maria.quispe@correo.pe")
                .telefono("966123456")
                .nombreComercial("Boutique Artesanal Huamanga")
                .giroNegocio("Venta de artesanías")
                .direccionEstablecimiento("Jr. 9 de Diciembre 142")
                .estado(EstadoExpediente.EN_EVALUACION)
                .fechaCreacion(LocalDateTime.now())
                .build());
    }

    @AfterEach
    void limpiar() {
        expedienteRepository.deleteById(expediente.getId());
    }

    @Test
    @DisplayName("El listado de expedientes exige autenticación")
    void listadoAnonimo() throws Exception {
        mockMvc.perform(get("/api/expedientes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El seguimiento público ofusca los datos personales del titular (Ley N° 29733)")
    void seguimientoOfuscado() throws Exception {
        mockMvc.perform(get("/api/expedientes/tramite/" + expediente.getNumeroTramite()).param("documento", "7891"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titularOfuscado").value(containsString("***")))
                .andExpect(jsonPath("$.documentoIdentidadOfuscado").value("42***91"))
                .andExpect(jsonPath("$.correoElectronico").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.voucherId").doesNotExist());
    }

    @Test
    @DisplayName("La licencia solo se descarga cuando ha sido emitida")
    void licenciaNoEmitida() throws Exception {
        String token = obtenerToken(mockMvc, objectMapper, EVALUADOR, CLAVE_EVALUADOR);
        mockMvc.perform(get("/api/expedientes/" + expediente.getId() + "/documentos/licencia").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La orden de pago solo existe con liquidación definitiva")
    void ordenDePagoSinLiquidacion() throws Exception {
        String token = obtenerToken(mockMvc, objectMapper, CIUDADANA_TITULAR, CLAVE_CIUDADANO);
        mockMvc.perform(get("/api/expedientes/" + expediente.getId() + "/orden-pago").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Los documentos del expediente no se descargan de forma anónima")
    void documentoAnonimo() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expediente.getId() + "/documentos/anexo1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El estado de salud es público y los demás puntos de gestión exigen el rol de administrador")
    void actuator() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
        String token = obtenerToken(mockMvc, objectMapper, EVALUADOR, CLAVE_EVALUADOR);
        mockMvc.perform(get("/actuator/beans").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }
}
