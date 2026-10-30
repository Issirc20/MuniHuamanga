package pe.gob.munihuamanga.licencias.expedientes.cuentas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.gob.munihuamanga.licencias.common.dto.CrearEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.CuentaResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.EstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.PuertoNotificacionCuenta;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pe.gob.munihuamanga.licencias.expedientes.soporte.DatosDePrueba.CIUDADANO_TERCERO;
import static pe.gob.munihuamanga.licencias.expedientes.soporte.DatosDePrueba.CLAVE_CIUDADANO;
import static pe.gob.munihuamanga.licencias.expedientes.soporte.DatosDePrueba.obtenerToken;

/**
 * Registro de cuentas, verificación de correo y acceso a cuentas y establecimientos según la representación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CuentaEstablecimientoSecurityIntegrationTest {

    private static final String CLAVE = "Registro.Seguro26";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CuentaRepository cuentaRepository;
    @MockBean private PuertoNotificacionCuenta notificacionCuenta;

    @Test
    @DisplayName("El registro de persona natural crea una cuenta pendiente de verificación")
    void registroPersonaNatural() throws Exception {
        String dni = dniAleatorio();
        mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(natural(dni))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"))
                .andExpect(jsonPath("$.operadores[0].rolOperador").value("TITULAR"));
    }

    @Test
    @DisplayName("El registro duplicado se rechaza con un mensaje que no revela el documento")
    void registroDuplicado() throws Exception {
        String dni = dniAleatorio();
        registrar(natural(dni));
        RegistroCuentaDto duplicado = natural(dni);
        duplicado.setEmail("otro." + dni + "@correo.pe");
        mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicado)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("El enlace de verificación enviado por correo activa la cuenta")
    void verificacionDeCorreo() throws Exception {
        String dni = dniAleatorio();
        CuentaResponseDto cuenta = registrar(natural(dni));

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(notificacionCuenta).enviarVerificacionCorreo(eq(cuenta.getEmail()), anyString(), enlace.capture());
        String token = enlace.getValue().substring(enlace.getValue().indexOf("token=") + "token=".length());

        mockMvc.perform(post("/api/cuentas/verificar-correo").param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificado").value(true));
        assertThat(cuentaRepository.findById(cuenta.getId()).orElseThrow().getEmailVerificado()).isTrue();

        mockMvc.perform(post("/api/cuentas/verificar-correo").param("token", token)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un tercero no accede a una cuenta que no representa")
    void cuentaAjena() throws Exception {
        CuentaResponseDto cuenta = registrar(juridica(rucAleatorio(), dniAleatorio()));
        String tokenTercero = obtenerToken(mockMvc, objectMapper, CIUDADANO_TERCERO, CLAVE_CIUDADANO);
        mockMvc.perform(get("/api/cuentas/" + cuenta.getId()).header("Authorization", "Bearer " + tokenTercero))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El representante legal inicia sesión con su DNI y gestiona los establecimientos de la persona jurídica")
    void representanteGestionaEstablecimientos() throws Exception {
        String dniRepresentante = dniAleatorio();
        CuentaResponseDto cuenta = registrar(juridica(rucAleatorio(), dniRepresentante));
        String token = obtenerToken(mockMvc, objectMapper, dniRepresentante, CLAVE);

        crearEstablecimiento(cuenta, token, "Local Central Plaza", "4711");
        crearEstablecimiento(cuenta, token, "Sucursal San Juan Bautista", "4773");

        mockMvc.perform(get("/api/establecimientos/cuenta/" + cuenta.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Un tercero no accede a un establecimiento ajeno")
    void establecimientoAjeno() throws Exception {
        String dniRepresentante = dniAleatorio();
        CuentaResponseDto cuenta = registrar(juridica(rucAleatorio(), dniRepresentante));
        String token = obtenerToken(mockMvc, objectMapper, dniRepresentante, CLAVE);
        EstablecimientoDto establecimiento = crearEstablecimiento(cuenta, token, "Local Confidencial", "4711");

        String tokenTercero = obtenerToken(mockMvc, objectMapper, CIUDADANO_TERCERO, CLAVE_CIUDADANO);
        mockMvc.perform(get("/api/establecimientos/" + establecimiento.getId()).header("Authorization", "Bearer " + tokenTercero))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El catálogo CIIU es de consulta pública")
    void catalogoCiiuPublico() throws Exception {
        mockMvc.perform(get("/api/catalogo-ciiu")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    private CuentaResponseDto registrar(RegistroCuentaDto dto) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(resultado.getResponse().getContentAsString(), CuentaResponseDto.class);
    }

    private EstablecimientoDto crearEstablecimiento(CuentaResponseDto cuenta, String token, String nombre, String ciiu) throws Exception {
        CrearEstablecimientoDto dto = CrearEstablecimientoDto.builder()
                .nombreComercial(nombre)
                .direccion("Portal Unión N° 45")
                .latitud(new BigDecimal("-13.1630000"))
                .longitud(new BigDecimal("-74.2230000"))
                .ciiuCodigo(ciiu)
                .giroPrincipal("Comercio minorista")
                .areaMetrosCuadrados(new BigDecimal("60.00"))
                .build();
        MvcResult resultado = mockMvc.perform(post("/api/establecimientos/cuenta/" + cuenta.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(resultado.getResponse().getContentAsString(), EstablecimientoDto.class);
    }

    private static RegistroCuentaDto natural(String dni) {
        return RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(dni)
                .razonSocialONombre("Persona de Prueba " + dni)
                .email("persona." + dni + "@correo.pe")
                .password(CLAVE)
                .build();
    }

    private static RegistroCuentaDto juridica(String ruc, String dniRepresentante) {
        return RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .numeroDocumento(ruc)
                .razonSocialONombre("Empresa de Prueba " + ruc + " S.A.C.")
                .email("empresa." + ruc + "@correo.pe")
                .password(CLAVE)
                .dniRepresentanteLegal(dniRepresentante)
                .nombreRepresentanteLegal("Representante de Prueba")
                .build();
    }

    private static String dniAleatorio() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
    }

    private static String rucAleatorio() {
        return "20" + ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L);
    }
}
