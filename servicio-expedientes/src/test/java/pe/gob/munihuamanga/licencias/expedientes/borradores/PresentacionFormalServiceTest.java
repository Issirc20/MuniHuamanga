package pe.gob.munihuamanga.licencias.expedientes.borradores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.PresentarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidacionRequisitosDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.DocumentoBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SolicitudBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.service.PresentacionFormalService;
import pe.gob.munihuamanga.licencias.expedientes.borradores.service.SecuenciaTramiteService;
import pe.gob.munihuamanga.licencias.expedientes.borradores.service.SolicitudBorradorService;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentacionFormalServiceTest {

    @Mock
    private SolicitudBorradorRepository borradorRepository;

    @Mock
    private SolicitudBorradorService borradorService;

    @Mock
    private SecuenciaTramiteService secuenciaTramiteService;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private OperadorCuentaRepository operadorCuentaRepository;

    @Mock
    private ExpedienteMapper expedienteMapper;

    @Mock
    private AuditoriaService auditoriaService;

    private PresentacionFormalService service;

    private Cuenta cuentaNatural;
    private Cuenta cuentaJuridica;
    private Establecimiento establecimientoTest;
    private SolicitudBorrador borradorApto;

    @BeforeEach
    void setUp() {
        service = new PresentacionFormalService(
                borradorRepository,
                borradorService,
                secuenciaTramiteService,
                expedienteRepository,
                operadorCuentaRepository,
                expedienteMapper,
                auditoriaService
        );

        UUID cuentaNatId = UUID.randomUUID();
        cuentaNatural = Cuenta.builder()
                .id(cuentaNatId)
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("42567891")
                .razonSocialONombre("María Quispe Huamán")
                .email("maria.quispe@test.pe")
                .estado(EstadoCuenta.ACTIVA)
                .emailVerificado(true)
                .build();

        UUID cuentaJurId = UUID.randomUUID();
        cuentaJuridica = Cuenta.builder()
                .id(cuentaJurId)
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .numeroDocumento("20601234567")
                .razonSocialONombre("INVERSIONES LOS RETABLOS S.A.C.")
                .email("contacto@retablos.pe")
                .estado(EstadoCuenta.ACTIVA)
                .emailVerificado(true)
                .build();

        establecimientoTest = Establecimiento.builder()
                .id(UUID.randomUUID())
                .cuenta(cuentaNatural)
                .codigoEstablecimiento("EST-001")
                .nombreComercial("Boutique Artesanal Huamanga")
                .direccion("Jr. 9 de Diciembre 142")
                .distrito("Ayacucho")
                .provincia("Huamanga")
                .departamento("Ayacucho")
                .giroPrincipal("Venta de artesanías y retablos")
                .areaMetrosCuadrados(new BigDecimal("35.50"))
                .build();

        List<DocumentoBorrador> docs = new ArrayList<>();
        docs.add(DocumentoBorrador.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE)
                .nombreArchivo("evidencia_itse.pdf")
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .fechaCarga(LocalDateTime.now())
                .cargadoPor(cuentaNatId)
                .build());

        docs.add(DocumentoBorrador.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumentoAdjunto.COMPROBANTE_PAGO_SAT)
                .nombreArchivo("recibo_sat.pdf")
                .hashSha256("d41d8cd98f00b204e9800998ecf8427e00000000000000000000000000000000")
                .fechaCarga(LocalDateTime.now())
                .cargadoPor(cuentaNatId)
                .build());

        borradorApto = SolicitudBorrador.builder()
                .id(UUID.randomUUID())
                .cuenta(cuentaNatural)
                .establecimiento(establecimientoTest)
                .creadorId(cuentaNatId)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .estado(EstadoBorrador.EN_EDICION)
                .version(0L)
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nombreComercialDeclarado("Boutique Artesanal Huamanga")
                .giroNegocioDeclarado("Venta de artesanías y retablos")
                .actividadDetallada("Venta minorista de retablos y artesanías tradicionales")
                .areaMetrosCuadrados(new BigDecimal("35.50"))
                .aforoPersonas(15)
                .numeroPisos(1)
                .horarioInicio("08:00")
                .horarioFin("20:00")
                .telefonoContacto("966123456")
                .correoNotificacion("maria.quispe@test.pe")
                .autorizaNotificacion(true)
                .nivelRiesgoDeclarado(NivelRiesgo.BAJO)
                .numeroOperacionSat("OP-SAT-2026-1234")
                .fechaPagoSat(LocalDate.now())
                .montoTasa(new BigDecimal("154.50"))
                .voucherId("VCH-2026-001")
                .documentos(docs)
                .build();
    }

    @Test
    @DisplayName("I04: Presentación formal exitosa genera número EXP-YYYY-XXXXX e instantánea")
    void presentacionFormal_Exitosa() {
        PresentarBorradorDto dto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .comentarios("Presentación formal de solicitud para galería artesanal")
                .build();

        ValidacionRequisitosDto validacionExitosa = ValidacionRequisitosDto.builder()
                .borradorId(borradorApto.getId())
                .aptoParaPresentacion(true)
                .camposFaltantes(List.of())
                .documentosFaltantes(List.of())
                .build();

        when(borradorRepository.findByIdWithLock(borradorApto.getId())).thenReturn(Optional.of(borradorApto));
        when(borradorService.validarRequisitos(borradorApto.getId())).thenReturn(validacionExitosa);
        when(secuenciaTramiteService.generarSiguienteNumeroExpediente()).thenReturn("EXP-2026-00101");
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpedienteResponseDto responseEsperado = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2026-00101")
                .estado(EstadoExpediente.PRESENTADO)
                .build();
        when(expedienteMapper.toDto(any(Expediente.class))).thenReturn(responseEsperado);

        ExpedienteResponseDto resultado = service.presentarBorrador(
                borradorApto.getId(),
                dto,
                cuentaNatural.getId(),
                "42567891"
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNumeroTramite()).isEqualTo("EXP-2026-00101");
        assertThat(resultado.getEstado()).isEqualTo(EstadoExpediente.PRESENTADO);
        assertThat(borradorApto.getEstado()).isEqualTo(EstadoBorrador.PRESENTADO);
        verify(auditoriaService).registrarTransicion(any(), eq(null), eq(EstadoExpediente.PRESENTADO), eq("42567891"), any());
    }

    @Test
    @DisplayName("I04: Idempotencia - Reintento de presentación retorna el mismo expediente sin duplicar")
    void presentacionFormal_Idempotente() {
        Expediente expPrevio = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00050")
                .estado(EstadoExpediente.PRESENTADO)
                .build();

        borradorApto.setEstado(EstadoBorrador.PRESENTADO);
        borradorApto.setExpediente(expPrevio);

        when(borradorRepository.findByIdWithLock(borradorApto.getId())).thenReturn(Optional.of(borradorApto));

        ExpedienteResponseDto dtoPrevio = ExpedienteResponseDto.builder()
                .id(expPrevio.getId())
                .numeroTramite("EXP-2026-00050")
                .estado(EstadoExpediente.PRESENTADO)
                .build();
        when(expedienteMapper.toDto(expPrevio)).thenReturn(dtoPrevio);

        PresentarBorradorDto dto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .build();

        ExpedienteResponseDto resultado = service.presentarBorrador(
                borradorApto.getId(),
                dto,
                cuentaNatural.getId(),
                "42567891"
        );

        assertThat(resultado.getNumeroTramite()).isEqualTo("EXP-2026-00050");
        verify(secuenciaTramiteService, never()).generarSiguienteNumeroExpediente(); // NO generó nuevo correlativo
        verify(expedienteRepository, never()).save(any()); // NO duplicó expediente
    }

    @Test
    @DisplayName("I04: Rechazo si la cuenta no tiene correo verificado")
    void presentacionFormal_RechazaCuentaSinVerificar() {
        cuentaNatural.setEmailVerificado(false);

        when(borradorRepository.findByIdWithLock(borradorApto.getId())).thenReturn(Optional.of(borradorApto));

        PresentarBorradorDto dto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .build();

        assertThatThrownBy(() -> service.presentarBorrador(borradorApto.getId(), dto, cuentaNatural.getId(), "42567891"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El correo electrónico de la cuenta no ha sido verificado");
    }

    @Test
    @DisplayName("I04: Rechazo si el operador de persona jurídica no tiene permiso PRESENTAR_EXPEDIENTE")
    void presentacionFormal_RechazaOperadorSinPermiso() {
        borradorApto.setCuenta(cuentaJuridica);
        establecimientoTest.setCuenta(cuentaJuridica);

        UUID operadorId = UUID.randomUUID();
        OperadorCuenta opSinPermiso = OperadorCuenta.builder()
                .id(UUID.randomUUID())
                .cuenta(cuentaJuridica)
                .dniOperador("41223344")
                .rolOperador(RolOperador.TRAMITADOR)
                .activo(true)
                .permisos(EnumSet.of(PermisoOperador.CREAR_BORRADOR, PermisoOperador.CONSULTAR_EXPEDIENTE)) // NO tiene PRESENTAR_EXPEDIENTE
                .build();

        when(borradorRepository.findByIdWithLock(borradorApto.getId())).thenReturn(Optional.of(borradorApto));
        when(operadorCuentaRepository.findByCuentaIdAndUsuarioId(cuentaJuridica.getId(), operadorId))
                .thenReturn(Optional.of(opSinPermiso));

        PresentarBorradorDto dto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .build();

        assertThatThrownBy(() -> service.presentarBorrador(borradorApto.getId(), dto, operadorId, "41223344"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("PRESENTAR_EXPEDIENTE");
    }

    @Test
    @DisplayName("I04.1: Rechazo de presentación para persona jurídica si no tiene representación legal acreditada (no basta RUC ni correo)")
    void presentacionFormal_RechazaPersonaJuridicaSinAcreditacionRepresentacion() {
        borradorApto.setCuenta(cuentaJuridica);
        establecimientoTest.setCuenta(cuentaJuridica);
        cuentaJuridica.setIdentidadValidadaOficialmente(false);

        UUID operadorId = UUID.randomUUID();
        OperadorCuenta opConPermiso = OperadorCuenta.builder()
                .id(UUID.randomUUID())
                .cuenta(cuentaJuridica)
                .dniOperador("41223344")
                .rolOperador(RolOperador.REPRESENTANTE_LEGAL)
                .activo(true)
                .permisos(EnumSet.of(PermisoOperador.PRESENTAR_EXPEDIENTE))
                .build();

        // Borrador no tiene Vigencia de poder
        borradorApto.setDocumentos(new ArrayList<>());

        when(borradorRepository.findByIdWithLock(borradorApto.getId())).thenReturn(Optional.of(borradorApto));
        when(operadorCuentaRepository.findByCuentaIdAndUsuarioId(cuentaJuridica.getId(), operadorId))
                .thenReturn(Optional.of(opConPermiso));

        PresentarBorradorDto dto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .build();

        assertThatThrownBy(() -> service.presentarBorrador(borradorApto.getId(), dto, operadorId, "41223344"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("representación legal mediante vigencia de poder registral");
    }

    @Test
    @DisplayName("I04.1: Acepta presentación para persona jurídica cuando adjunta Vigencia de Poder de SUNARP")
    void presentacionFormal_AceptaPersonaJuridicaConVigenciaPoder() {
        borradorApto.setCuenta(cuentaJuridica);
        establecimientoTest.setCuenta(cuentaJuridica);
        cuentaJuridica.setIdentidadValidadaOficialmente(false);

        UUID operadorId = UUID.randomUUID();
        OperadorCuenta opConPermiso = OperadorCuenta.builder()
                .id(UUID.randomUUID())
                .cuenta(cuentaJuridica)
                .dniOperador("41223344")
                .rolOperador(RolOperador.REPRESENTANTE_LEGAL)
                .activo(true)
                .permisos(EnumSet.of(PermisoOperador.PRESENTAR_EXPEDIENTE))
                .build();

        List<DocumentoBorrador> docs = new ArrayList<>();
        docs.add(DocumentoBorrador.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumentoAdjunto.VIGENCIA_PODER)
                .nombreArchivo("vigencia_poder_sunarp.pdf")
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .fechaCarga(LocalDateTime.now())
                .cargadoPor(operadorId)
                .build());
        borradorApto.setDocumentos(docs);

        when(borradorRepository.findByIdWithLock(borradorApto.getId())).thenReturn(Optional.of(borradorApto));
        when(operadorCuentaRepository.findByCuentaIdAndUsuarioId(cuentaJuridica.getId(), operadorId))
                .thenReturn(Optional.of(opConPermiso));
        when(borradorService.validarRequisitos(borradorApto.getId())).thenReturn(ValidacionRequisitosDto.builder()
                .borradorId(borradorApto.getId())
                .aptoParaPresentacion(true)
                .camposFaltantes(List.of())
                .documentosFaltantes(List.of())
                .build());
        when(secuenciaTramiteService.generarSiguienteNumeroExpediente()).thenReturn("EXP-2026-00001");
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expedienteMapper.toDto(any(Expediente.class))).thenReturn(ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2026-00001")
                .estado(EstadoExpediente.PRESENTADO)
                .build());

        PresentarBorradorDto dto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .build();

        ExpedienteResponseDto resp = service.presentarBorrador(borradorApto.getId(), dto, operadorId, "41223344");
        assertThat(resp).isNotNull();
        assertThat(resp.getNumeroTramite()).isEqualTo("EXP-2026-00001");
    }
}
