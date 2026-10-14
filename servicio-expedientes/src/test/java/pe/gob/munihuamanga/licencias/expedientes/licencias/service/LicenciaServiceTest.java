package pe.gob.munihuamanga.licencias.expedientes.licencias.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pe.gob.munihuamanga.licencias.common.dto.EmitirLicenciaDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.LicenciaEmitidaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.LicenciaEmitida;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.HistorialEmisionLicenciaRepository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.ResolucionAdministrativaRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import pe.gob.munihuamanga.licencias.expedientes.service.LicenciaPdfGenerator;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class LicenciaServiceTest {

    @Mock
    private LicenciaEmitidaRepository licenciaRepository;
    @Mock
    private HistorialEmisionLicenciaRepository historialEmisionRepository;
    @Mock
    private ResolucionAdministrativaRepository resolucionRepository;
    @Mock
    private ExpedienteRepository expedienteRepository;
    @Mock
    private EstablecimientoRepository establecimientoRepository;
    @Mock
    private SecuenciaLicenciaService secuenciaLicenciaService;
    @Mock
    private LicenciaPdfGenerator licenciaPdfGenerator;
    @Mock
    private DocumentoPdfService documentoPdfService;
    @Mock
    private ExpedienteMapper expedienteMapper;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private NotificacionEmailService notificacionEmailService;
    @Mock
    private pe.gob.munihuamanga.licencias.expedientes.rectificacion.repository.SolicitudRectificacionUbicacionRepository solicitudRectificacionUbicacionRepository;
    @Mock
    private pe.gob.munihuamanga.licencias.expedientes.licencias.repository.AuditoriaConsultaInspeccionRepository auditoriaConsultaInspeccionRepository;
    @Mock
    private Authentication auth;

    @InjectMocks
    private LicenciaService licenciaService;

    private UUID resolucionId;
    private ResolucionAdministrativa resolucion;
    private Expediente expediente;

    @BeforeEach
    void setUp() {
        resolucionId = UUID.randomUUID();
        expediente = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00999")
                .nombreTitular("Juan Perez Huaman")
                .tipoDocumento(TipoDocumento.DNI)
                .documentoIdentidad("40506070")
                .nombreComercial("Libreria Escolar")
                .direccionEstablecimiento("Jr. Asamblea 456")
                .giroNegocio("Venta de libros y útiles de oficina")
                .ciiuCodigo("4761")
                .areaMetrosCuadrados(new BigDecimal("65.50"))
                .aforoPersonas(20)
                .zonificacion("Comercio Zonal (CZ)")
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .estado(EstadoExpediente.APTO_PARA_RESOLUCION)
                .build();

        resolucion = ResolucionAdministrativa.builder()
                .id(resolucionId)
                .numeroResolucion("RES-2026-00001")
                .expediente(expediente)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .estado(EstadoResolucion.FIRMADA)
                .hashDocumento("d41d8cd98f00b204e9800998ecf8427e")
                .build();
    }

    @Test
    @DisplayName("I08-09: No permite emitir licencia si la resolución no está FIRMADA digitalmente")
    void testEmitirLicencia_SinFirmaDigital_LanzaExcepcion() {
        resolucion.setEstado(EstadoResolucion.APROBADA_PENDIENTE_FIRMA);
        when(resolucionRepository.findByIdWithLock(resolucionId)).thenReturn(Optional.of(resolucion));

        EmitirLicenciaDto dto = EmitirLicenciaDto.builder()
                .resolucionId(resolucionId)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                licenciaService.emitirLicencia(dto, auth));
        assertTrue(ex.getMessage().contains("no cuenta con firma digital institucional válida"));
    }

    @Test
    @DisplayName("I08-10: No permite emitir licencia para una resolución con decisión DENEGATORIA")
    void testEmitirLicencia_ResolucionDenegatoria_LanzaExcepcion() {
        resolucion.setTipoDecision(TipoDecisionResolucion.DENEGATORIA);
        when(resolucionRepository.findByIdWithLock(resolucionId)).thenReturn(Optional.of(resolucion));

        EmitirLicenciaDto dto = EmitirLicenciaDto.builder()
                .resolucionId(resolucionId)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                licenciaService.emitirLicencia(dto, auth));
        assertTrue(ex.getMessage().contains("No procede emitir licencia para una resolución con decisión DENEGATORIA"));
    }

    @Test
    @DisplayName("I08-11: Emisión exitosa de licencia genera correlativo LIC-YYYY-XXXXX, QR no enumerable, PDF y pasa expediente a APROBADO")
    void testEmitirLicencia_Exitoso() {
        when(auth.getName()).thenReturn("admin");
        when(resolucionRepository.findByIdWithLock(resolucionId)).thenReturn(Optional.of(resolucion));
        when(licenciaRepository.findByResolucionId(resolucionId)).thenReturn(Optional.empty());
        when(licenciaRepository.findByExpedienteId(expediente.getId())).thenReturn(Optional.empty());
        when(secuenciaLicenciaService.generarSiguienteNumeroLicencia()).thenReturn("LIC-2026-00001");
        when(documentoPdfService.generarImagenQr(any(), eq(180), eq(180))).thenReturn("qr_png_bytes".getBytes());
        when(expedienteMapper.toDto(expediente)).thenReturn(new ExpedienteResponseDto());
        when(licenciaPdfGenerator.generarLicencia(any(), any())).thenReturn("pdf_licencia_oficial".getBytes());
        when(licenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historialEmisionRepository.findByLicenciaIdOrderByFechaRegistroDesc(any())).thenReturn(Collections.emptyList());

        EmitirLicenciaDto dto = EmitirLicenciaDto.builder()
                .resolucionId(resolucionId)
                .tipoVigencia(TipoVigenciaLicencia.INDETERMINADA)
                .observacionesEmision("Licencia otorgada conforme al TUPA.")
                .build();

        LicenciaEmitidaDto result = licenciaService.emitirLicencia(dto, auth);

        assertNotNull(result);
        assertEquals("LIC-2026-00001", result.getNumeroLicencia());
        assertEquals(EstadoLicencia.EMITIDA, result.getEstadoLicencia());
        assertNotNull(result.getCodigoVerificacionQr());
        assertEquals(32, result.getCodigoVerificacionQr().length()); // UUID sin guiones
        assertEquals(EstadoExpediente.APROBADO, expediente.getEstado());
        assertEquals("LIC-2026-00001", expediente.getNumeroLicencia());
        assertTrue(expediente.getFirmaDigitalValida());

        verify(expedienteRepository).save(expediente);
        verify(auditoriaService).registrarTransicion(eq(expediente.getId()), eq(EstadoExpediente.APTO_PARA_RESOLUCION), eq(EstadoExpediente.APROBADO), eq("admin"), any());
        verify(notificacionEmailService).notificarAprobacion(expediente);
    }

    @Test
    @DisplayName("I08-12: Emisión concurrente o reintento HTTP es idempotente y no genera duplicados")
    void testEmitirLicencia_Idempotente() {
        LicenciaEmitida licExistente = LicenciaEmitida.builder()
                .id(UUID.randomUUID())
                .numeroLicencia("LIC-2026-00001")
                .resolucion(resolucion)
                .expediente(expediente)
                .codigoVerificacionQr("AABBCCDDEEFF11223344556677889900")
                .estado(EstadoLicencia.EMITIDA)
                .tipoVigencia(TipoVigenciaLicencia.INDETERMINADA)
                .fechaEmision(LocalDate.now())
                .autoridadEmisora("Subgerencia de Comercio y Licencias")
                .hashPdf("sha256")
                .titularTipoDocumento("DNI")
                .titularNumeroDocumento("40506070")
                .titularNombreORazonSocial("Juan Perez")
                .nombreComercial("Bodega")
                .direccionEstablecimiento("Jr. 28 de Julio")
                .giroActividad("Bodega")
                .build();

        when(resolucionRepository.findByIdWithLock(resolucionId)).thenReturn(Optional.of(resolucion));
        when(licenciaRepository.findByResolucionId(resolucionId)).thenReturn(Optional.of(licExistente));
        when(historialEmisionRepository.findByLicenciaIdOrderByFechaRegistroDesc(any())).thenReturn(Collections.emptyList());

        EmitirLicenciaDto dto = EmitirLicenciaDto.builder()
                .resolucionId(resolucionId)
                .build();

        LicenciaEmitidaDto result = licenciaService.emitirLicencia(dto, auth);

        assertNotNull(result);
        assertEquals("LIC-2026-00001", result.getNumeroLicencia());
        assertEquals("AABBCCDDEEFF11223344556677889900", result.getCodigoVerificacionQr());
    }

    @Test
    @DisplayName("I08-13: Descarga de PDF de licencia oficial genera documento con QR")
    void testDescargarPdfLicencia() {
        UUID licenciaId = UUID.randomUUID();
        LicenciaEmitida lic = LicenciaEmitida.builder()
                .id(licenciaId)
                .numeroLicencia("LIC-2026-00001")
                .resolucion(resolucion)
                .expediente(expediente)
                .codigoVerificacionQr("AABBCCDDEEFF11223344556677889900")
                .fechaEmision(LocalDate.now())
                .build();

        when(licenciaRepository.findById(licenciaId)).thenReturn(Optional.of(lic));
        when(documentoPdfService.generarImagenQr(any(), eq(180), eq(180))).thenReturn("qr_png".getBytes());
        when(expedienteMapper.toDto(expediente)).thenReturn(new ExpedienteResponseDto());
        when(licenciaPdfGenerator.generarLicencia(any(), any())).thenReturn("pdf_licencia".getBytes());

        byte[] pdf = licenciaService.descargarPdfLicencia(licenciaId);
        assertNotNull(pdf);
        assertEquals("pdf_licencia", new String(pdf));
    }
}
