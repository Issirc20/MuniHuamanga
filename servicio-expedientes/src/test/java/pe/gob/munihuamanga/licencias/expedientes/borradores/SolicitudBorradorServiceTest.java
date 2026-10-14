package pe.gob.munihuamanga.licencias.expedientes.borradores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.DocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitudBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidacionRequisitosDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.DocumentoBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.DocumentoBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SolicitudBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.service.SolicitudBorradorService;
import pe.gob.munihuamanga.licencias.expedientes.borradores.security.DocumentoSecurityValidator;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudBorradorServiceTest {

    @Mock
    private SolicitudBorradorRepository borradorRepository;

    @Mock
    private DocumentoBorradorRepository documentoRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private EstablecimientoRepository establecimientoRepository;

    private SolicitudBorradorService service;

    private Cuenta cuentaTest;
    private Establecimiento establecimientoTest;
    private SolicitudBorrador borradorTest;

    @BeforeEach
    void setUp() {
        service = new SolicitudBorradorService(
                borradorRepository,
                documentoRepository,
                cuentaRepository,
                establecimientoRepository,
                new DocumentoSecurityValidator()
        );

        UUID cuentaId = UUID.randomUUID();
        cuentaTest = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("42567891")
                .razonSocialONombre("Juan Carlos Pérez")
                .email("juan.perez@test.pe")
                .estado(EstadoCuenta.ACTIVA)
                .emailVerificado(true)
                .build();

        establecimientoTest = Establecimiento.builder()
                .id(UUID.randomUUID())
                .cuenta(cuentaTest)
                .codigoEstablecimiento("EST-001")
                .nombreComercial("Bodega Juanito")
                .direccion("Jr. 28 de Julio 123")
                .distrito("Ayacucho")
                .giroPrincipal("Bodega y Abarrotes")
                .areaMetrosCuadrados(new BigDecimal("45.00"))
                .build();

        borradorTest = SolicitudBorrador.builder()
                .id(UUID.randomUUID())
                .cuenta(cuentaTest)
                .establecimiento(establecimientoTest)
                .creadorId(cuentaId)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .estado(EstadoBorrador.EN_EDICION)
                .version(0L)
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nombreComercialDeclarado("Bodega Juanito")
                .giroNegocioDeclarado("Bodega y Abarrotes")
                .areaMetrosCuadrados(new BigDecimal("45.00"))
                .documentos(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("I04: Crear borrador exitosamente sin generar correlativo EXP-*")
    void crearBorrador_Exitoso_NoGeneraNumeroExpediente() {
        CrearBorradorDto dto = CrearBorradorDto.builder()
                .cuentaId(cuentaTest.getId())
                .establecimientoId(establecimientoTest.getId())
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nombreComercialDeclarado("Bodega Juanito")
                .giroNegocioDeclarado("Bodega y Abarrotes")
                .areaMetrosCuadrados(new BigDecimal("45.00"))
                .build();

        when(cuentaRepository.findById(cuentaTest.getId())).thenReturn(Optional.of(cuentaTest));
        when(establecimientoRepository.findById(establecimientoTest.getId())).thenReturn(Optional.of(establecimientoTest));
        when(borradorRepository.save(any(SolicitudBorrador.class))).thenAnswer(inv -> inv.getArgument(0));

        SolicitudBorradorDto resultado = service.crearBorrador(dto, cuentaTest.getId());

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoBorrador.EN_EDICION);
        assertThat(resultado.getNumeroTramiteFormal()).isNull(); // NO genera número formal EXP-*
        assertThat(resultado.getExpedienteId()).isNull();
        assertThat(resultado.getAvisoLegal()).contains("borrador");
    }

    @Test
    @DisplayName("I04: Guardar avance parcial actualiza campos y timestamp")
    void guardarAvance_Exitoso() {
        ActualizarBorradorDto dto = ActualizarBorradorDto.builder()
                .aforoPersonas(20)
                .horarioInicio("08:00")
                .horarioFin("20:00")
                .telefonoContacto("966123456")
                .correoNotificacion("notificaciones@test.pe")
                .autorizaNotificacion(true)
                .nivelRiesgoDeclarado(NivelRiesgo.BAJO)
                .numeroOperacionSat("SAT-2026-999")
                .montoTasa(new BigDecimal("154.50"))
                .voucherId("VCH-999")
                .build();

        when(borradorRepository.findById(borradorTest.getId())).thenReturn(Optional.of(borradorTest));
        when(borradorRepository.save(any(SolicitudBorrador.class))).thenAnswer(inv -> inv.getArgument(0));

        SolicitudBorradorDto actualizado = service.guardarAvance(borradorTest.getId(), dto, cuentaTest.getId());

        assertThat(actualizado.getAforoPersonas()).isEqualTo(20);
        assertThat(actualizado.getNivelRiesgoDeclarado()).isEqualTo(NivelRiesgo.BAJO);
        assertThat(actualizado.getNumeroOperacionSat()).isEqualTo("SAT-2026-999");
        assertThat(actualizado.getAvisoPagoSat()).contains("NO constituye validación oficial");
    }

    @Test
    @DisplayName("I04: Adjuntar documento probatorio calcula y almacena hash SHA-256")
    void adjuntarDocumento_CalculaSha256Inmutable() {
        String contenidoPrueba = "%PDF-1.4 Evidencia de riesgo ITSE y plano Huamanga";
        String base64 = Base64.getEncoder().encodeToString(contenidoPrueba.getBytes());

        AdjuntarDocumentoBorradorDto dto = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE)
                .nombreArchivo("evidencia_itse.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(base64)
                .descripcion("Inspección técnica preliminar")
                .build();

        when(borradorRepository.findById(borradorTest.getId())).thenReturn(Optional.of(borradorTest));
        when(documentoRepository.save(any(DocumentoBorrador.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borradorRepository.save(any(SolicitudBorrador.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoBorradorDto docCreado = service.adjuntarDocumento(borradorTest.getId(), dto, cuentaTest.getId());

        assertThat(docCreado).isNotNull();
        assertThat(docCreado.getHashSha256()).isNotNull().hasSize(64);
        assertThat(docCreado.getTipoDocumento()).isEqualTo(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE);
        assertThat(docCreado.getAvisoLegal()).contains("NO equivale a una certificación");
    }

    @Test
    @DisplayName("I04: Validar requisitos detecta campos y documentos obligatorios faltantes")
    void validarRequisitos_DetectaFaltantes() {
        when(borradorRepository.findByIdWithDocumentos(borradorTest.getId())).thenReturn(Optional.of(borradorTest));

        ValidacionRequisitosDto validacion = service.validarRequisitos(borradorTest.getId());

        assertThat(validacion.isAptoParaPresentacion()).isFalse();
        assertThat(validacion.getCamposFaltantes()).contains("Nivel de riesgo ITSE declarado", "Número de operación / constancia de pago SAT");
        assertThat(validacion.getDocumentosFaltantes()).contains("Evidencia técnica o anexo de clasificación de riesgo ITSE");
    }

    @Test
    @DisplayName("I04: Descartar borrador conserva la auditoría y fecha de descarte")
    void descartarBorrador_ConservaAuditoria() {
        when(borradorRepository.findById(borradorTest.getId())).thenReturn(Optional.of(borradorTest));
        when(borradorRepository.save(any(SolicitudBorrador.class))).thenAnswer(inv -> inv.getArgument(0));

        service.descartarBorrador(borradorTest.getId(), "Desistimiento voluntario del administrado", cuentaTest.getId());

        assertThat(borradorTest.getEstado()).isEqualTo(EstadoBorrador.DESCARTADO);
        assertThat(borradorTest.getMotivoDescarte()).isEqualTo("Desistimiento voluntario del administrado");
        assertThat(borradorTest.getFechaDescarte()).isNotNull();
    }

    @Test
    @DisplayName("I04.1: Rechazo de archivo con extensión no permitida (.exe, .sh)")
    void adjuntarDocumento_RechazaExtensionNoPermitida() {
        String base64 = Base64.getEncoder().encodeToString("MZ malicious binary".getBytes());
        AdjuntarDocumentoBorradorDto dto = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE)
                .nombreArchivo("malware.exe")
                .contenidoBase64(base64)
                .build();

        when(borradorRepository.findById(borradorTest.getId())).thenReturn(Optional.of(borradorTest));

        assertThatThrownBy(() -> service.adjuntarDocumento(borradorTest.getId(), dto, cuentaTest.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Extensión de archivo no permitida");
    }

    @Test
    @DisplayName("I04.1: Rechazo de archivo con extensión PDF pero contenido sin magic number %PDF-")
    void adjuntarDocumento_RechazaMagicNumberInvalido() {
        String base64 = Base64.getEncoder().encodeToString("Esto es texto plano disfrazado de pdf".getBytes());
        AdjuntarDocumentoBorradorDto dto = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE)
                .nombreArchivo("falso_documento.pdf")
                .contenidoBase64(base64)
                .build();

        when(borradorRepository.findById(borradorTest.getId())).thenReturn(Optional.of(borradorTest));

        assertThatThrownBy(() -> service.adjuntarDocumento(borradorTest.getId(), dto, cuentaTest.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no corresponde a un documento PDF legítimo");
    }

    @Test
    @DisplayName("I04.1: Sanitización estricta contra Path Traversal en nombres de archivo")
    void adjuntarDocumento_SanitizaPathTraversal() {
        String contenidoPrueba = "%PDF-1.4 Archivo con path traversal";
        String base64 = Base64.getEncoder().encodeToString(contenidoPrueba.getBytes());
        AdjuntarDocumentoBorradorDto dto = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE)
                .nombreArchivo("../../../etc/shadow_report.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(base64)
                .build();

        when(borradorRepository.findById(borradorTest.getId())).thenReturn(Optional.of(borradorTest));
        when(documentoRepository.save(any(DocumentoBorrador.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoBorradorDto doc = service.adjuntarDocumento(borradorTest.getId(), dto, cuentaTest.getId());

        assertThat(doc.getNombreArchivo()).doesNotContain("..");
        assertThat(doc.getNombreArchivo()).doesNotContain("/");
        assertThat(doc.getNombreArchivo()).doesNotContain("\\");
        assertThat(doc.getNombreArchivo()).endsWith(".pdf");
    }

    @Test
    @DisplayName("I04.1: Persona jurídica sin representación acreditada detecta documento de Vigencia de Poder faltante")
    void validarRequisitos_PersonaJuridicaExigeVigenciaPoder() {
        Cuenta cuentaJuridica = Cuenta.builder()
                .id(UUID.randomUUID())
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .numeroDocumento("20601234567")
                .razonSocialONombre("EMPRESA COMERCIAL S.A.C.")
                .email("admin@empresa.pe")
                .emailVerificado(true)
                .identidadValidadaOficialmente(false)
                .build();

        borradorTest.setCuenta(cuentaJuridica);

        when(borradorRepository.findByIdWithDocumentos(borradorTest.getId())).thenReturn(Optional.of(borradorTest));
        when(documentoRepository.findByBorradorId(borradorTest.getId())).thenReturn(List.of());

        ValidacionRequisitosDto validacion = service.validarRequisitos(borradorTest.getId());

        assertThat(validacion.isAptoParaPresentacion()).isFalse();
        assertThat(validacion.getDocumentosFaltantes()).anyMatch(d -> d.contains("Vigencia de poder"));
    }
}
