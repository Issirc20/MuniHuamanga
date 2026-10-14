package pe.gob.munihuamanga.licencias.expedientes.resoluciones.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.AprobarResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.ChecklistAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolucionAdministrativaDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitarFirmaDto;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.ChecklistAdministrativoService;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.FirmaDigitalPort;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.ResultadoFirmaDigital;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.HistorialResolucionRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.ResolucionAdministrativaRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolucionAdministrativaServiceTest {

    @Mock
    private ResolucionAdministrativaRepository resolucionRepository;
    @Mock
    private HistorialResolucionRepository historialResolucionRepository;
    @Mock
    private ExpedienteRepository expedienteRepository;
    @Mock
    private InformeAdministrativoRepository informeAdministrativoRepository;
    @Mock
    private ChecklistAdministrativoService checklistAdministrativoService;
    @Mock
    private SecuenciaResolucionService secuenciaResolucionService;
    @Mock
    private ResolucionPdfGenerator resolucionPdfGenerator;
    @Mock
    private FirmaDigitalPort firmaDigitalPort;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.OperacionFirmaRepository operacionFirmaRepository;
    @Mock
    private Authentication auth;

    @InjectMocks
    private ResolucionAdministrativaService resolucionService;

    private UUID expedienteId;
    private Expediente expediente;
    private InformeAdministrativo informe;

    @BeforeEach
    void setUp() {
        expedienteId = UUID.randomUUID();
        expediente = Expediente.builder()
                .id(expedienteId)
                .numeroTramite("EXP-2026-00100")
                .nombreTitular("Empresa Los Andes S.A.C.")
                .documentoIdentidad("20601234567")
                .nombreComercial("Bodega Los Andes")
                .direccionEstablecimiento("Jr. 28 de Julio 123")
                .estado(EstadoExpediente.APTO_PARA_RESOLUCION)
                .build();

        informe = InformeAdministrativo.builder()
                .id(UUID.randomUUID())
                .numeroInforme("INF-2026-00001")
                .expediente(expediente)
                .version(1)
                .evaluadorId(UUID.randomUUID())
                .evaluadorNombre("evaluador_juan")
                .fechaElaboracion(LocalDateTime.now())
                .conclusion(ConclusionInforme.FAVORABLE)
                .estado(EstadoInforme.APROBADO)
                .build();
    }

    @Test
    @DisplayName("I08-01: No permite crear resolución si el expediente no tiene informe administrativo aprobado")
    void testCrearBorrador_SinInformeAprobado_LanzaExcepcion() {
        informe.setEstado(EstadoInforme.EN_REVISION);
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(informeAdministrativoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)).thenReturn(Optional.of(informe));

        CrearResolucionDto dto = CrearResolucionDto.builder()
                .expedienteId(expedienteId)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .fundamentos("Fundamentos de prueba legales y administrativos.")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                resolucionService.crearBorrador(dto, auth));
        assertTrue(ex.getMessage().contains("sin informe administrativo previamente APROBADO"));
    }

    @Test
    @DisplayName("I08-02: No permite resolución FAVORABLE si el expediente no está en APTO_PARA_RESOLUCION")
    void testCrearBorrador_DecisionFavorable_ExpedienteNoApto_LanzaExcepcion() {
        expediente.setEstado(EstadoExpediente.EN_EVALUACION_FINAL);
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(informeAdministrativoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)).thenReturn(Optional.of(informe));

        CrearResolucionDto dto = CrearResolucionDto.builder()
                .expedienteId(expedienteId)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .fundamentos("Fundamentos de prueba legales y administrativos.")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                resolucionService.crearBorrador(dto, auth));
        assertTrue(ex.getMessage().contains("debe encontrarse en estado APTO_PARA_RESOLUCION"));
    }

    @Test
    @DisplayName("I08-03: Crear borrador exitoso genera correlativo oficial RES-YYYY-XXXXX y hash de borrador")
    void testCrearBorrador_Exitoso() {
        when(auth.getName()).thenReturn("funcionario_res");
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(informeAdministrativoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)).thenReturn(Optional.of(informe));
        when(checklistAdministrativoService.generarChecklist(expedienteId)).thenReturn(
                ChecklistAdministrativoDto.builder().aptoParaInformeFavorable(true).build());
        when(resolucionRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)).thenReturn(Optional.empty());
        when(secuenciaResolucionService.generarSiguienteNumeroResolucion()).thenReturn("RES-2026-00001");
        when(resolucionPdfGenerator.generarResolucionPdf(any())).thenReturn("pdf_bytes".getBytes());
        when(resolucionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historialResolucionRepository.findByResolucionIdOrderByFechaRegistroDesc(any())).thenReturn(Collections.emptyList());

        CrearResolucionDto dto = CrearResolucionDto.builder()
                .expedienteId(expedienteId)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .fundamentos("Cumple cabalmente con todos los recaudos exigidos por el TUPA.")
                .referenciasLegales("Ley N° 27972, Ley N° 28976")
                .build();

        ResolucionAdministrativaDto result = resolucionService.crearBorrador(dto, auth);

        assertNotNull(result);
        assertEquals("RES-2026-00001", result.getNumeroResolucion());
        assertEquals(EstadoResolucion.BORRADOR, result.getEstadoResolucion());
        assertNotNull(result.getHashDocumento());
        verify(resolucionRepository).save(any());
    }

    @Test
    @DisplayName("I08-04: Segregación de funciones: El evaluador que formuló el informe técnico no puede aprobar la resolución")
    void testAprobarResolucion_MismoEvaluador_Rechazado() {
        UUID evaluadorId = UUID.randomUUID();
        informe.setEvaluadorId(evaluadorId);
        informe.setEvaluadorNombre("evaluador_juan");

        ResolucionAdministrativa res = ResolucionAdministrativa.builder()
                .id(UUID.randomUUID())
                .numeroResolucion("RES-2026-00001")
                .expediente(expediente)
                .informeAdministrativo(informe)
                .estado(EstadoResolucion.BORRADOR)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .fundamentos("Fundamentos de prueba")
                .build();

        when(auth.getName()).thenReturn("evaluador_juan");
        when(usuarioRepository.findByUsername("evaluador_juan")).thenReturn(
                Optional.of(Usuario.builder().id(evaluadorId).username("evaluador_juan").build()));
        when(resolucionRepository.findByIdWithLock(res.getId())).thenReturn(Optional.of(res));

        AprobarResolucionDto dto = AprobarResolucionDto.builder()
                .cargoAutoridad("Gerente de Desarrollo Económico")
                .observaciones("Aprobado")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                resolucionService.aprobarResolucion(res.getId(), dto, auth));
        assertTrue(ex.getMessage().contains("Segregación de funciones"));
    }

    @Test
    @DisplayName("I08-05: Aprobación formal por autoridad competente pasa a APROBADA_PENDIENTE_FIRMA y sella hash")
    void testAprobarResolucion_AutoridadCompetente_Exito() {
        UUID gerenteId = UUID.randomUUID();
        ResolucionAdministrativa res = ResolucionAdministrativa.builder()
                .id(UUID.randomUUID())
                .numeroResolucion("RES-2026-00001")
                .expediente(expediente)
                .informeAdministrativo(informe)
                .estado(EstadoResolucion.BORRADOR)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .fundamentos("Fundamentos de prueba")
                .version(1)
                .esVersionActual(true)
                .creadoPor("funcionario")
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(auth.getName()).thenReturn("gerente_carlos");
        when(usuarioRepository.findByUsername("gerente_carlos")).thenReturn(
                Optional.of(Usuario.builder().id(gerenteId).username("gerente_carlos").build()));
        when(resolucionRepository.findByIdWithLock(res.getId())).thenReturn(Optional.of(res));
        when(checklistAdministrativoService.generarChecklist(expedienteId)).thenReturn(
                ChecklistAdministrativoDto.builder().aptoParaInformeFavorable(true).build());
        when(resolucionPdfGenerator.generarResolucionPdf(any())).thenReturn("pdf_aprobado_bytes".getBytes());
        when(resolucionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historialResolucionRepository.findByResolucionIdOrderByFechaRegistroDesc(any())).thenReturn(Collections.emptyList());

        AprobarResolucionDto dto = AprobarResolucionDto.builder()
                .cargoAutoridad("Gerente de Desarrollo Económico")
                .observaciones("Aprobado formalmente")
                .build();

        ResolucionAdministrativaDto result = resolucionService.aprobarResolucion(res.getId(), dto, auth);

        assertNotNull(result);
        assertEquals(EstadoResolucion.APROBADA_PENDIENTE_FIRMA, result.getEstadoResolucion());
        assertEquals("gerente_carlos", result.getAutoridadCompetenteNombre());
        assertNotNull(result.getHashDocumento());
    }

    @Test
    @DisplayName("I08-06: Si el servicio institucional de firma no está configurado, la resolución queda retenida y lanza excepción informativa")
    void testSolicitarFirma_SinServicioInstitucional_QuedaRetenida() {
        ResolucionAdministrativa res = ResolucionAdministrativa.builder()
                .id(UUID.randomUUID())
                .numeroResolucion("RES-2026-00001")
                .expediente(expediente)
                .informeAdministrativo(informe)
                .estado(EstadoResolucion.APROBADA_PENDIENTE_FIRMA)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .build();

        when(auth.getName()).thenReturn("gerente_carlos");
        when(resolucionRepository.findByIdWithLock(res.getId())).thenReturn(Optional.of(res));
        when(firmaDigitalPort.estaDisponible()).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                resolucionService.solicitarFirmaDigital(res.getId(), new SolicitarFirmaDto(), auth));

        assertTrue(ex.getMessage().contains("El servicio institucional de firma digital no se encuentra disponible"));
        assertEquals(EstadoResolucion.APROBADA_PENDIENTE_FIRMA, res.getEstado());
        verify(resolucionRepository).save(res);
    }

    @Test
    @DisplayName("I08-07: Con proveedor institucional activo, la resolución transiciona a FIRMADA con hash y metadatos oficiales")
    void testSolicitarFirma_ConProveedorActivo_PasaAFirmada() {
        ResolucionAdministrativa res = ResolucionAdministrativa.builder()
                .id(UUID.randomUUID())
                .numeroResolucion("RES-2026-00001")
                .expediente(expediente)
                .informeAdministrativo(informe)
                .estado(EstadoResolucion.APROBADA_PENDIENTE_FIRMA)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .autoridadCompetenteCargo("Gerente de Desarrollo Económico")
                .version(1)
                .esVersionActual(true)
                .creadoPor("funcionario")
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(auth.getName()).thenReturn("gerente_carlos");
        when(resolucionRepository.findByIdWithLock(res.getId())).thenReturn(Optional.of(res));
        when(firmaDigitalPort.estaDisponible()).thenReturn(true);
        when(resolucionPdfGenerator.generarResolucionPdf(any())).thenReturn("pdf_bytes".getBytes());

        ResultadoFirmaDigital resFirma = ResultadoFirmaDigital.builder()
                .pdfFirmado("pdf_firmado".getBytes())
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .nombreFirmante("Carlos Gerente")
                .cargoFirmante("Gerente de Desarrollo Económico")
                .entidadCertificadora("RENIEC PKI")
                .fechaFirma(LocalDateTime.now())
                .esValida(true)
                .build();

        when(firmaDigitalPort.firmarResolucion(any(), eq("RES-2026-00001"), any(), any())).thenReturn(resFirma);
        when(resolucionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historialResolucionRepository.findByResolucionIdOrderByFechaRegistroDesc(any())).thenReturn(Collections.emptyList());

        ResolucionAdministrativaDto result = resolucionService.solicitarFirmaDigital(res.getId(), new SolicitarFirmaDto(), auth);

        assertNotNull(result);
        assertEquals(EstadoResolucion.FIRMADA, result.getEstadoResolucion());
        assertEquals("Carlos Gerente", result.getFirmanteNombre());
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result.getHashDocumento());
    }

    @Test
    @DisplayName("I08-08: Solicitud de firma es idempotente si la resolución ya se encuentra FIRMADA")
    void testSolicitarFirma_Idempotente() {
        ResolucionAdministrativa res = ResolucionAdministrativa.builder()
                .id(UUID.randomUUID())
                .numeroResolucion("RES-2026-00001")
                .expediente(expediente)
                .informeAdministrativo(informe)
                .estado(EstadoResolucion.FIRMADA)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .version(1)
                .esVersionActual(true)
                .creadoPor("funcionario")
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(auth.getName()).thenReturn("gerente_carlos");
        when(resolucionRepository.findByIdWithLock(res.getId())).thenReturn(Optional.of(res));
        when(historialResolucionRepository.findByResolucionIdOrderByFechaRegistroDesc(any())).thenReturn(Collections.emptyList());

        ResolucionAdministrativaDto result = resolucionService.solicitarFirmaDigital(res.getId(), new SolicitarFirmaDto(), auth);

        assertNotNull(result);
        assertEquals(EstadoResolucion.FIRMADA, result.getEstadoResolucion());
    }
}
