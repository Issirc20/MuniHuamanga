package pe.gob.munihuamanga.licencias.expedientes.cuentas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarPermisosOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CuentaResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.EstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CuentaEstablecimientoSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    private String tokenCiudadanoTitular;
    private String tokenCiudadanoTercero;

    @BeforeEach
    void setUp() throws Exception {
        tokenCiudadanoTitular = obtenerToken("42567891", "ciud123");
        tokenCiudadanoTercero = obtenerToken("78999999", "ciud123");
    }

    private String obtenerToken(String username, String password) throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto loginResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponseDto.class
        );
        return loginResponse.getToken();
    }

    @Test
    @DisplayName("I03: Registro público de cuenta natural por DNI con contraseña cifrada")
    void registrarCuentaNatural_Exitoso() throws Exception {
        String dniRandom = "33" + String.format("%06d", (int) (Math.random() * 900000));
        RegistroCuentaDto dto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(dniRandom)
                .razonSocialONombre("Carlos Alberto Mendoza")
                .email("carlos." + dniRandom + "@test.pe")
                .password("PasswordSeguro2026!")
                .build();

        mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroDocumento").value(dniRandom))
                .andExpect(jsonPath("$.emailVerificado").value(false))
                .andExpect(jsonPath("$.avisoIdentidad").value(containsString("declarativo")));
    }

    @Test
    @DisplayName("I03: Registro público de cuenta jurídica por RUC")
    void registrarCuentaJuridica_Exitoso() throws Exception {
        String rucRandom = "20" + String.format("%09d", (int) (Math.random() * 900000000));
        RegistroCuentaDto dto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .numeroDocumento(rucRandom)
                .razonSocialONombre("Comercial Los Andes Ayacucho S.A.C.")
                .email("contacto." + rucRandom + "@losandes.pe")
                .password("EmpresaSegura2026!")
                .nombreRepresentanteLegal("Guillermo Quispe")
                .dniRepresentanteLegal("41223344")
                .build();

        MvcResult result = mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroDocumento").value(rucRandom))
                .andExpect(jsonPath("$.operadores", hasSize(1)))
                .andExpect(jsonPath("$.operadores[0].dniOperador").value("41223344"))
                .andExpect(jsonPath("$.operadores[0].rolOperador").value("REPRESENTANTE_LEGAL"))
                .andReturn();
    }

    @Test
    @DisplayName("I03: Intento de registro con documento duplicado es rechazado")
    void registrarCuentaDuplicada_RetornaError() throws Exception {
        String dniPrueba = "48888888";
        RegistroCuentaDto dto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(dniPrueba)
                .razonSocialONombre("Intento Duplicado")
                .email("duplicado@test.pe")
                .password("Pass12345678!")
                .build();

        // Registrar primero
        if (!cuentaRepository.existsByNumeroDocumento(dniPrueba)) {
            mockMvc.perform(post("/api/cuentas/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        // Segundo intento con mismo DNI: debe ser rechazado con 400 Bad Request
        mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I03: Verificación de correo electrónico con token")
    void verificarCorreo_Exitoso() throws Exception {
        String dniRandom = "34" + String.format("%06d", (int) (Math.random() * 900000));
        RegistroCuentaDto dto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(dniRandom)
                .razonSocialONombre("Rosa María Flores")
                .email("rosa." + dniRandom + "@test.pe")
                .password("PasswordSeguro2026!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        CuentaResponseDto cuenta = objectMapper.readValue(result.getResponse().getContentAsString(), CuentaResponseDto.class);
        String token = cuentaRepository.findById(cuenta.getId()).orElseThrow().getTokenVerificacionEmail();

        mockMvc.perform(post("/api/cuentas/verificar-correo")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificado").value(true));
    }

    @Test
    @DisplayName("I03: Consulta de cuenta ajena por un ciudadano tercero es rechazada con 403 Forbidden")
    void accesoACuentaAjena_Retorna403() throws Exception {
        String rucRandom = "20" + String.format("%09d", (int) (Math.random() * 900000000));
        RegistroCuentaDto dto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .numeroDocumento(rucRandom)
                .razonSocialONombre("Empresa Privada S.A.")
                .email("info." + rucRandom + "@empresa.pe")
                .password("PasswordEmpresa123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        CuentaResponseDto cuenta = objectMapper.readValue(result.getResponse().getContentAsString(), CuentaResponseDto.class);

        // Intento de acceso con token de ciudadano tercero
        mockMvc.perform(get("/api/cuentas/" + cuenta.getId())
                        .header("Authorization", "Bearer " + tokenCiudadanoTercero))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I03: Registro y consulta de múltiples establecimientos para una misma cuenta (RUC)")
    void registrarMultiplesEstablecimientos_Exitoso() throws Exception {
        String rucRandom = "20" + String.format("%09d", (int) (Math.random() * 900000000));
        RegistroCuentaDto dtoCuenta = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .numeroDocumento(rucRandom)
                .razonSocialONombre("Cadena Comercial Huamanga S.A.C.")
                .email("cadena." + rucRandom + "@comercio.pe")
                .password("PasswordCadena123!")
                .build();

        MvcResult resultCuenta = mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoCuenta)))
                .andExpect(status().isCreated())
                .andReturn();

        CuentaResponseDto cuenta = objectMapper.readValue(resultCuenta.getResponse().getContentAsString(), CuentaResponseDto.class);
        String tokenTitularEmpresa = obtenerToken(rucRandom, "PasswordCadena123!");

        // Establecimiento 1
        CrearEstablecimientoDto dtoEst1 = CrearEstablecimientoDto.builder()
                .nombreComercial("Local Central Plaza")
                .direccion("Portal Unión N° 45")
                .latitud(new BigDecimal("-13.1630000"))
                .longitud(new BigDecimal("-74.2230000"))
                .ciiuCodigo("4711")
                .giroPrincipal("Minimarket y Abarrotes")
                .areaMetrosCuadrados(new BigDecimal("120.00"))
                .build();

        mockMvc.perform(post("/api/establecimientos/cuenta/" + cuenta.getId())
                        .header("Authorization", "Bearer " + tokenTitularEmpresa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEst1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoEstablecimiento").value("EST-001"))
                .andExpect(jsonPath("$.advertenciaZonificacion").value(containsString("no constituyen zonificación municipal aprobada")));

        // Establecimiento 2 (segunda sucursal)
        CrearEstablecimientoDto dtoEst2 = CrearEstablecimientoDto.builder()
                .nombreComercial("Sucursal San Juan Bautista")
                .direccion("Av. Cuzco N° 780")
                .latitud(new BigDecimal("-13.1680000"))
                .longitud(new BigDecimal("-74.2200000"))
                .ciiuCodigo("4773")
                .giroPrincipal("Venta de Retablos")
                .areaMetrosCuadrados(new BigDecimal("45.00"))
                .build();

        mockMvc.perform(post("/api/establecimientos/cuenta/" + cuenta.getId())
                        .header("Authorization", "Bearer " + tokenTitularEmpresa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEst2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoEstablecimiento").value("EST-002"));

        // Listar establecimientos de la cuenta
        mockMvc.perform(get("/api/establecimientos/cuenta/" + cuenta.getId())
                        .header("Authorization", "Bearer " + tokenTitularEmpresa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("I03: Intento de consulta de establecimiento ajeno es rechazado con 403 Forbidden")
    void accesoAEstablecimientoAjeno_Retorna403() throws Exception {
        String rucRandom = "20" + String.format("%09d", (int) (Math.random() * 900000000));
        RegistroCuentaDto dtoCuenta = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .numeroDocumento(rucRandom)
                .razonSocialONombre("Negocio Confidencial S.A.C.")
                .email("privado." + rucRandom + "@empresa.pe")
                .password("PasswordPrivado123!")
                .build();

        MvcResult resultCuenta = mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoCuenta)))
                .andExpect(status().isCreated())
                .andReturn();

        CuentaResponseDto cuenta = objectMapper.readValue(resultCuenta.getResponse().getContentAsString(), CuentaResponseDto.class);
        String tokenTitular = obtenerToken(rucRandom, "PasswordPrivado123!");

        CrearEstablecimientoDto dtoEst = CrearEstablecimientoDto.builder()
                .nombreComercial("Local Confidencial")
                .direccion("Jr. Bellido N° 210")
                .latitud(new BigDecimal("-13.1620000"))
                .longitud(new BigDecimal("-74.2240000"))
                .ciiuCodigo("4711")
                .giroPrincipal("Comercio")
                .areaMetrosCuadrados(new BigDecimal("60.00"))
                .build();

        MvcResult resultEst = mockMvc.perform(post("/api/establecimientos/cuenta/" + cuenta.getId())
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEst)))
                .andExpect(status().isCreated())
                .andReturn();

        EstablecimientoDto est = objectMapper.readValue(resultEst.getResponse().getContentAsString(), EstablecimientoDto.class);

        // Intento de acceso de ciudadano ajeno
        mockMvc.perform(get("/api/establecimientos/" + est.getId())
                        .header("Authorization", "Bearer " + tokenCiudadanoTercero))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I03: Consulta pública de Catálogo CIIU administrable")
    void consultarCatalogoCiiuPublico_Retorna200() throws Exception {
        mockMvc.perform(get("/api/catalogo-ciiu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
