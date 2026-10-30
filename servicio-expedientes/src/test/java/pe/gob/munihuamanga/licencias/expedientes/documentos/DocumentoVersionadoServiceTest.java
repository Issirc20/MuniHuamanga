package pe.gob.munihuamanga.licencias.expedientes.documentos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.security.ServicioRepresentacion;
import pe.gob.munihuamanga.licencias.expedientes.security.VerificadorTitularidad;
import java.util.EnumSet;
import static org.mockito.Mockito.lenient;
import org.springframework.security.access.AccessDeniedException;
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoVersionadoDto;
import pe.gob.munihuamanga.licencias.common.dto.DocumentoVersionadoDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.DocumentoBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.DocumentoBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.security.DocumentoSecurityValidator;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.service.DocumentoVersionadoService;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoVersionadoServiceTest {

    @Mock
    private DocumentoVersionadoRepository documentoRepository;

    @Mock
    private DocumentoBorradorRepository documentoBorradorRepository;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private OperadorCuentaRepository operadorCuentaRepository;

    @Mock
    private pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository cuentaRepository;

    @Mock
    private pe.gob.munihuamanga.licencias.expedientes.documentos.repository.ObservacionExpedienteRepository observacionRepository;

    @Mock
    private DocumentoSecurityValidator securityValidator;

    @Mock
    private ProveedorActor proveedorActor;

    @Mock
    private ServicioRepresentacion servicioRepresentacion;

    private DocumentoVersionadoService service;

    private UUID expedienteId;
    private UUID titularId;
    private Expediente expedienteObservado;
    private Expediente expedienteAprobado;

    // Archivo PDF válido mínimo (%PDF- ...)
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4 sample content".getBytes();
    private static final String VALID_BASE64 = Base64.getEncoder().encodeToString(VALID_PDF_BYTES);
    private static final String MOCK_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @BeforeEach
    void setUp() {
        expedienteId = UUID.randomUUID();
        titularId = UUID.randomUUID();

        service = new DocumentoVersionadoService(documentoRepository, documentoBorradorRepository, expedienteRepository,
                observacionRepository, securityValidator, new VerificadorTitularidad(proveedorActor, servicioRepresentacion));
        lenient().when(proveedorActor.actual()).thenReturn(new Actor(titularId, "42567891", "Titular de prueba", EnumSet.of(RolUsuario.ROLE_CIUDADANO)));
        lenient().when(servicioRepresentacion.tieneFacultad(any(), any(), any(PermisoOperador[].class))).thenReturn(true);

        expedienteObservado = Expediente.builder()
                .id(expedienteId)
                .numeroTramite("EXP-2026-00101")
                .solicitanteId(titularId)
                .documentoIdentidad("42567891")
                .estado(EstadoExpediente.OBSERVADO)
                .build();

        expedienteAprobado = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00050")
                .solicitanteId(titularId)
                .documentoIdentidad("42567891")
                .estado(EstadoExpediente.APROBADO)
                .build();
    }

    @Test
    @DisplayName("Debe adjuntar versión 1 si es el primer documento de ese tipo")
    void adjuntarPrimeraVersion_exito() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteObservado));
        when(securityValidator.validarYSanitizar(eq("plano.pdf"), any(), any())).thenReturn("plano.pdf");
        when(securityValidator.calcularSha256(any())).thenReturn(MOCK_SHA256);
        when(documentoRepository.findFirstByExpedienteIdAndTipoDocumentoOrderByVersionDesc(expedienteId, TipoDocumentoAdjunto.PLANO_ARQUITECTURA))
                .thenReturn(Optional.empty());
        when(documentoRepository.save(any(DocumentoVersionado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdjuntarDocumentoVersionadoDto dto = AdjuntarDocumentoVersionadoDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.PLANO_ARQUITECTURA)
                .nombreArchivo("plano.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(VALID_BASE64)
                .motivoActualizacion("Carga inicial")
                .build();

        DocumentoVersionadoDto res = service.adjuntarDocumentoAExpediente(expedienteId, dto);

        assertThat(res).isNotNull();
        assertThat(res.getVersion()).isEqualTo(1);
        assertThat(res.getVersionAnteriorId()).isNull();
        assertThat(res.getEsVersionActual()).isTrue();
        assertThat(res.getHashSha256()).isEqualTo(MOCK_SHA256);
    }

    @Test
    @DisplayName("Debe incrementar la versión (v2) y marcar la versión anterior como no actual")
    void adjuntarNuevaVersion_incrementaVersionYConservaAnterior() {
        UUID docV1Id = UUID.randomUUID();
        DocumentoVersionado docV1 = DocumentoVersionado.builder()
                .id(docV1Id)
                .expedienteId(expedienteId)
                .tipoDocumento(TipoDocumentoAdjunto.PLANO_ARQUITECTURA)
                .version(1)
                .nombreArchivo("plano_v1.pdf")
                .hashSha256("hash_v1")
                .esVersionActual(true)
                .build();

        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteObservado));
        when(securityValidator.validarYSanitizar(eq("plano_v2.pdf"), any(), any())).thenReturn("plano_v2.pdf");
        when(securityValidator.calcularSha256(any())).thenReturn("hash_v2_sha256");
        when(documentoRepository.findFirstByExpedienteIdAndTipoDocumentoOrderByVersionDesc(expedienteId, TipoDocumentoAdjunto.PLANO_ARQUITECTURA))
                .thenReturn(Optional.of(docV1));
        when(documentoRepository.save(any(DocumentoVersionado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdjuntarDocumentoVersionadoDto dto = AdjuntarDocumentoVersionadoDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.PLANO_ARQUITECTURA)
                .nombreArchivo("plano_v2.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(VALID_BASE64)
                .motivoActualizacion("Subsanación de observación sobre escala")
                .build();

        DocumentoVersionadoDto res = service.adjuntarDocumentoAExpediente(expedienteId, dto);

        assertThat(res).isNotNull();
        assertThat(res.getVersion()).isEqualTo(2);
        assertThat(res.getVersionAnteriorId()).isEqualTo(docV1Id);
        assertThat(res.getEsVersionActual()).isTrue();
        assertThat(docV1.getEsVersionActual()).isFalse();
    }

    @Test
    @DisplayName("Debe impedir alteración de documentos en expedientes concluidos o resueltos")
    void adjuntarDocumento_expedienteConcluido_rechaza() {
        when(expedienteRepository.findById(expedienteAprobado.getId())).thenReturn(Optional.of(expedienteAprobado));

        AdjuntarDocumentoVersionadoDto dto = AdjuntarDocumentoVersionadoDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.PLANO_ARQUITECTURA)
                .nombreArchivo("plano.pdf")
                .contenidoBase64(VALID_BASE64)
                .build();

        assertThatThrownBy(() -> service.adjuntarDocumentoAExpediente(expedienteAprobado.getId(), dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pueden agregar documentos a un expediente resuelto");
    }

    @Test
    @DisplayName("Debe rechazar a ciudadano intentando adjuntar documentos si no está en estado OBSERVADO")
    void adjuntarDocumento_ciudadanoEnEstadoNoObservado_rechaza() {
        Expediente expPresentado = Expediente.builder()
                .id(UUID.randomUUID())
                .solicitanteId(titularId)
                .documentoIdentidad("42567891")
                .estado(EstadoExpediente.PRESENTADO)
                .build();

        when(expedienteRepository.findById(expPresentado.getId())).thenReturn(Optional.of(expPresentado));

        AdjuntarDocumentoVersionadoDto dto = AdjuntarDocumentoVersionadoDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.PLANO_ARQUITECTURA)
                .nombreArchivo("plano.pdf")
                .contenidoBase64(VALID_BASE64)
                .build();

        assertThatThrownBy(() -> service.adjuntarDocumentoAExpediente(expPresentado.getId(), dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no admite la carga del documento");
    }

    @Test
    @DisplayName("Debe rechazar el acceso por IDOR a documentos de otro administrado")
    void descargarDocumento_usuarioAjeno_deniegaAcceso() {
        UUID docId = UUID.randomUUID();
        DocumentoVersionado doc = DocumentoVersionado.builder()
                .id(docId)
                .expedienteId(expedienteId)
                .nombreArchivo("plano.pdf")
                .hashSha256(MOCK_SHA256)
                .build();

        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(servicioRepresentacion.tieneFacultad(any(), any(), any(PermisoOperador[].class))).thenReturn(false);
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteObservado));

        UUID otroUsuarioId = UUID.randomUUID();
        assertThatThrownBy(() -> service.descargarDocumento(docId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No cuenta con autorización");
    }

    @Test
    @DisplayName("Debe migrar correctamente documentos de borrador a versión 1 en el expediente")
    void migrarDocumentosBorrador_creaVersionUno() {
        UUID borradorId = UUID.randomUUID();
        DocumentoBorrador docBorrador = DocumentoBorrador.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumentoAdjunto.CROQUIS_UBICACION)
                .nombreArchivo("croquis.pdf")
                .tipoContenido("application/pdf")
                .tamanioBytes(1024L)
                .hashSha256("sha256_croquis")
                .cargadoPor(titularId)
                .build();

        when(documentoBorradorRepository.findByBorradorId(borradorId)).thenReturn(List.of(docBorrador));

        service.migrarDocumentosBorradorAExpediente(borradorId, expedienteId, titularId, "42567891");

        verify(documentoRepository).save(any(DocumentoVersionado.class));
    }

    @Test
    @DisplayName("I05.1: Debe rechazar intento de vincular una observación perteneciente a otro expediente")
    void adjuntarDocumento_conObservacionAjena_lanzaIllegalArgumentException() {
        UUID obsAjenaId = UUID.randomUUID();
        pe.gob.munihuamanga.licencias.expedientes.documentos.model.ObservacionExpediente obsAjena =
                pe.gob.munihuamanga.licencias.expedientes.documentos.model.ObservacionExpediente.builder()
                        .id(obsAjenaId)
                        .expedienteId(UUID.randomUUID()) // Otro expediente
                        .requisitoAfectado("PLANO_DISTRIBUCION")
                        .motivo("Observación ajena")
                        .build();

        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteObservado));
        when(observacionRepository.findById(obsAjenaId)).thenReturn(Optional.of(obsAjena));

        AdjuntarDocumentoVersionadoDto dto = AdjuntarDocumentoVersionadoDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.PLANO_DISTRIBUCION)
                .nombreArchivo("plano.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(VALID_BASE64)
                .observacionId(obsAjenaId)
                .build();

        assertThatThrownBy(() -> service.adjuntarDocumentoAExpediente(expedienteId, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece al expediente");
    }

    @Test
    @DisplayName("I05.1: Al adjuntar nueva versión debe invocar desmarcarVersionesActualesExpediente para atomicidad")
    void adjuntarDocumento_desmarcaVersionesPreviasDeFormaAtomica() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expedienteObservado));
        when(securityValidator.validarYSanitizar(any(), any(), any())).thenReturn("plano_sanitizado.pdf");
        when(securityValidator.calcularSha256(any())).thenReturn(MOCK_SHA256);
        when(documentoRepository.findFirstByExpedienteIdAndTipoDocumentoOrderByVersionDesc(eq(expedienteId), eq(TipoDocumentoAdjunto.PLANO_DISTRIBUCION)))
                .thenReturn(Optional.empty());
        when(documentoRepository.save(any(DocumentoVersionado.class))).thenAnswer(inv -> inv.getArgument(0));

        AdjuntarDocumentoVersionadoDto dto = AdjuntarDocumentoVersionadoDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.PLANO_DISTRIBUCION)
                .nombreArchivo("plano.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(VALID_BASE64)
                .build();

        service.adjuntarDocumentoAExpediente(expedienteId, dto);

        verify(documentoRepository).desmarcarVersionesActualesExpediente(expedienteId, TipoDocumentoAdjunto.PLANO_DISTRIBUCION);
    }

    @Test
    @DisplayName("I05.1: Expediente histórico V1 sin titularidad verificada (sin cuentaId ni solicitanteId) es denegado a ciudadano")
    void validarTitularidad_expedienteV1SinReclamar_deniegaAccesoACiudadano() {
        UUID expV1Id = UUID.randomUUID();
        Expediente expV1 = Expediente.builder()
                .id(expV1Id)
                .numeroTramite("EXP-2023-00050")
                .solicitanteId(null) // V1 no reclamado
                .cuentaId(null)      // V1 no reclamado
                .documentoIdentidad("42567891") // Mismo DNI
                .estado(EstadoExpediente.OBSERVADO)
                .build();

        UUID docId = UUID.randomUUID();
        DocumentoVersionado doc = DocumentoVersionado.builder()
                .id(docId)
                .expedienteId(expV1Id)
                .nombreArchivo("plano_v1.pdf")
                .hashSha256(MOCK_SHA256)
                .build();

        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(servicioRepresentacion.tieneFacultad(any(), any(), any(PermisoOperador[].class))).thenReturn(false);
        when(expedienteRepository.findById(expV1Id)).thenReturn(Optional.of(expV1));

        // Ciudadano con DNI 42567891 intenta acceder a expediente V1 no reclamado
        assertThatThrownBy(() -> service.descargarDocumento(docId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No cuenta con autorización");
    }
}
