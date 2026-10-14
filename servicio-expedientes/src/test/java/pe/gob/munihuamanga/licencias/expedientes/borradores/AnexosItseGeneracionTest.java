package pe.gob.munihuamanga.licencias.expedientes.borradores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SolicitudBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.service.SolicitudBorradorService;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnexosItseGeneracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitudBorradorService borradorService;

    @Autowired
    private DocumentoPdfService documentoPdfService;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    @Autowired
    private SolicitudBorradorRepository borradorRepository;

    private SolicitudBorrador borrador;
    private final String CIUDADANO_DNI = "43777701";

    @BeforeEach
    void setUp() {
        UUID cuentaId = UUID.randomUUID();
        Cuenta cuenta = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(CIUDADANO_DNI)
                .razonSocialONombre("Maria Elena Huaman")
                .email("maria.huaman@test.gob.pe")
                .emailVerificado(true)
                .activo(true)
                .estado(EstadoCuenta.ACTIVA)
                .fechaCreacion(LocalDateTime.now())
                .build();
        cuenta = cuentaRepository.save(cuenta);

        Establecimiento est = Establecimiento.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .codigoEstablecimiento("EST-001")
                .nombreComercial("Farmacia Santa Rosa")
                .direccion("Av. Mariscal Caceres 820")
                .distrito("Ayacucho")
                .provincia("Huamanga")
                .departamento("Ayacucho")
                .latitud(new BigDecimal("-13.1630000"))
                .longitud(new BigDecimal("-74.2230000"))
                .giroPrincipal("Venta de productos farmaceuticos")
                .areaMetrosCuadrados(new BigDecimal("95.50"))
                .activo(true)
                .fechaRegistro(LocalDateTime.now())
                .build();
        est = establecimientoRepository.save(est);

        borrador = SolicitudBorrador.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .establecimiento(est)
                .creadorId(cuentaId)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .estado(EstadoBorrador.EN_EDICION)
                .version(1L)
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nombreComercialDeclarado("Farmacia Santa Rosa - Sucursal 1")
                .giroNegocioDeclarado("Venta de productos farmaceuticos y de tocador")
                .actividadDetallada("Comercialización de medicinas y productos de higiene")
                .areaMetrosCuadrados(new BigDecimal("95.50"))
                .aforoPersonas(20)
                .numeroPisos(1)
                .horarioInicio("08:00")
                .horarioFin("22:00")
                .telefonoContacto("966112233")
                .correoNotificacion("farmacia.santarosa@test.gob.pe")
                .autorizaNotificacion(true)
                .nivelRiesgoDeclarado(NivelRiesgo.BAJO)
                .montoTasa(new BigDecimal("154.50"))
                .numeroOperacionSat("OP-99881122")
                .build();
        borrador = borradorRepository.save(borrador);
    }

    @Test
    @DisplayName("Conversión de borrador a DTO reutiliza datos de cuenta y establecimiento")
    void conversionBorradorADto_reutilizaDatosCompletos() {
        ExpedienteResponseDto dto = borradorService.convertirBorradorAExpedienteResponseDto(borrador.getId());

        assertThat(dto).isNotNull();
        assertThat(dto.getNombreTitular()).isEqualTo("Maria Elena Huaman");
        assertThat(dto.getDocumentoIdentidad()).isEqualTo(CIUDADANO_DNI);
        assertThat(dto.getNombreComercial()).isEqualTo("Farmacia Santa Rosa - Sucursal 1");
        assertThat(dto.getDireccionEstablecimiento()).isEqualTo("Av. Mariscal Caceres 820");
        assertThat(dto.getNivelRiesgo()).isEqualTo(NivelRiesgo.BAJO);
    }

    @Test
    @DisplayName("Generación directa en PDF de Anexo 1, 3 y 4 produce documentos válidos (%PDF-)")
    void generacionPdfAnexos_producePdfLegitimo() {
        ExpedienteResponseDto dto = borradorService.convertirBorradorAExpedienteResponseDto(borrador.getId());

        byte[] anexo1Pdf = documentoPdfService.generarAnexo1DeclaracionJurada(dto);
        assertThat(anexo1Pdf).isNotEmpty().startsWith("%PDF-".getBytes());

        byte[] anexo3Pdf = documentoPdfService.generarAnexo3MatrizRiesgoItse(dto);
        assertThat(anexo3Pdf).isNotEmpty().startsWith("%PDF-".getBytes());

        byte[] anexo4Pdf = documentoPdfService.generarAnexo4CondicionesSeguridad(dto);
        assertThat(anexo4Pdf).isNotEmpty().startsWith("%PDF-".getBytes());
    }

    @Test
    @WithMockUser(username = "43777701", roles = {"CIUDADANO"})
    @DisplayName("Endpoint REST descarga Anexo 1 Declaración Jurada desde borrador")
    void descargarAnexo1Borrador_restEndpoint() throws Exception {
        mockMvc.perform(get("/api/borradores/{id}/formularios/anexo-1/pdf", borrador.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Anexo1-DeclaracionJurada-")));
    }

    @Test
    @WithMockUser(username = "43777701", roles = {"CIUDADANO"})
    @DisplayName("Endpoint REST descarga Anexo 3 Matriz de Riesgo ITSE desde borrador")
    void descargarAnexo3Borrador_restEndpoint() throws Exception {
        mockMvc.perform(get("/api/borradores/{id}/formularios/anexo-3/pdf", borrador.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Anexo3-MatrizRiesgoITSE-")));
    }

    @Test
    @WithMockUser(username = "43777701", roles = {"CIUDADANO"})
    @DisplayName("Endpoint REST descarga Anexo 4 Condiciones de Seguridad ITSE desde borrador")
    void descargarAnexo4Borrador_restEndpoint() throws Exception {
        mockMvc.perform(get("/api/borradores/{id}/formularios/anexo-4/pdf", borrador.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Anexo4-CondicionesSeguridad-")));
    }
}
