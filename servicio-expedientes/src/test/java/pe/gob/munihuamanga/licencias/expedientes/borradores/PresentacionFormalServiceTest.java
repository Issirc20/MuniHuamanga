package pe.gob.munihuamanga.licencias.expedientes.borradores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
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
import pe.gob.munihuamanga.licencias.expedientes.documentos.service.DocumentoVersionadoService;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.ServicioRepresentacion;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentacionFormalServiceTest {

    @Mock private SolicitudBorradorRepository borradorRepository;
    @Mock private SolicitudBorradorService borradorService;
    @Mock private SecuenciaTramiteService secuenciaTramiteService;
    @Mock private ExpedienteRepository expedienteRepository;
    @Mock private ExpedienteMapper expedienteMapper;
    @Mock private AuditoriaService auditoriaService;
    @Mock private DocumentoVersionadoService documentoVersionadoService;
    @Mock private ProveedorActor proveedorActor;
    @Mock private ServicioRepresentacion servicioRepresentacion;
    @Mock private NotificacionEmailService notificacionEmailService;

    private PresentacionFormalService service;
    private Cuenta cuenta;
    private Establecimiento establecimiento;
    private SolicitudBorrador borrador;
    private final UUID personaId = UUID.randomUUID();
    private final PresentarBorradorDto confirmacion = PresentarBorradorDto.builder()
            .declaracionJuradaAceptada(true).confirmacionPresentacion(true).build();

    @BeforeEach
    void setUp() {
        service = new PresentacionFormalService(borradorRepository, borradorService, secuenciaTramiteService,
                expedienteRepository, expedienteMapper, auditoriaService, documentoVersionadoService,
                proveedorActor, servicioRepresentacion, notificacionEmailService);

        cuenta = Cuenta.builder()
                .id(UUID.randomUUID())
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("42567891")
                .razonSocialONombre("María Quispe Huamán")
                .estado(EstadoCuenta.ACTIVA)
                .emailVerificado(true)
                .build();
        establecimiento = Establecimiento.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .nombreComercial("Boutique Artesanal Huamanga")
                .direccion("Jr. 9 de Diciembre 142")
                .giroPrincipal("Venta de artesanías")
                .build();
        borrador = SolicitudBorrador.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .establecimiento(establecimiento)
                .creadorId(personaId)
                .estado(EstadoBorrador.EN_EDICION)
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nombreComercialDeclarado("Boutique Artesanal Huamanga")
                .giroNegocioDeclarado("Venta de artesanías")
                .areaMetrosCuadrados(new BigDecimal("35.50"))
                .documentos(new ArrayList<>())
                .build();

        lenient().when(proveedorActor.actual()).thenReturn(ciudadano());
        lenient().when(servicioRepresentacion.tieneFacultad(eq(personaId), eq(cuenta.getId()), any(PermisoOperador[].class))).thenReturn(true);
    }

    @Test
    @DisplayName("La presentación crea el expediente en PRESENTADO, sin pago ni nivel de riesgo, y sin fecha límite")
    void presentacionExitosa() {
        prepararPresentacionApta();

        service.presentarBorrador(borrador.getId(), confirmacion);

        ArgumentCaptor<Expediente> creado = ArgumentCaptor.forClass(Expediente.class);
        verify(expedienteRepository).save(creado.capture());
        assertThat(creado.getValue().getEstado()).isEqualTo(EstadoExpediente.PRESENTADO);
        assertThat(creado.getValue().getNivelRiesgo()).isNull();
        assertThat(creado.getValue().getMontoTasa()).isNull();
        assertThat(creado.getValue().getVoucherId()).isNull();
        assertThat(creado.getValue().getFechaPagoSat()).isNull();
        assertThat(creado.getValue().getFechaLimite()).isNull();
        assertThat(borrador.getEstado()).isEqualTo(EstadoBorrador.PRESENTADO);
        verify(auditoriaService).registrarTransicion(any(), eq(null), eq(EstadoExpediente.PRESENTADO), any(), any());
    }

    @Test
    @DisplayName("La presentación es idempotente: un reintento devuelve el mismo expediente")
    void presentacionIdempotente() {
        Expediente previo = Expediente.builder().id(UUID.randomUUID()).numeroTramite("EXP-2026-00050").estado(EstadoExpediente.PRESENTADO).build();
        borrador.setEstado(EstadoBorrador.PRESENTADO);
        borrador.setExpediente(previo);
        when(borradorRepository.findByIdWithLock(borrador.getId())).thenReturn(Optional.of(borrador));
        when(expedienteMapper.toDto(previo)).thenReturn(ExpedienteResponseDto.builder().numeroTramite("EXP-2026-00050").build());

        assertThat(service.presentarBorrador(borrador.getId(), confirmacion).getNumeroTramite()).isEqualTo("EXP-2026-00050");
        verify(secuenciaTramiteService, never()).generarSiguienteNumeroExpediente();
        verify(expedienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("La presentación virtual exige el correo verificado")
    void rechazaCorreoNoVerificado() {
        cuenta.setEmailVerificado(false);
        when(borradorRepository.findByIdWithLock(borrador.getId())).thenReturn(Optional.of(borrador));

        assertThatThrownBy(() -> service.presentarBorrador(borrador.getId(), confirmacion))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("verificar su correo");
    }

    @Test
    @DisplayName("Mesa de partes puede presentar en ventanilla aunque el correo del administrado no esté verificado")
    void presentacionPresencial() {
        cuenta.setEmailVerificado(false);
        cuenta.setEstado(EstadoCuenta.PENDIENTE_VERIFICACION);
        when(proveedorActor.actual()).thenReturn(new Actor(UUID.randomUUID(), "mesapartes", "Mesa de Partes", EnumSet.of(RolUsuario.ROLE_MESA_DE_PARTES)));
        prepararPresentacionApta();

        service.presentarBorrador(borrador.getId(), confirmacion);

        verify(expedienteRepository).save(any(Expediente.class));
    }

    @Test
    @DisplayName("Una persona sin la facultad de presentar no puede formalizar la solicitud")
    void rechazaSinFacultad() {
        when(borradorRepository.findByIdWithLock(borrador.getId())).thenReturn(Optional.of(borrador));
        when(servicioRepresentacion.tieneFacultad(eq(personaId), eq(cuenta.getId()), any(PermisoOperador[].class))).thenReturn(false);

        assertThatThrownBy(() -> service.presentarBorrador(borrador.getId(), confirmacion)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Una persona jurídica sin vigencia de poder ni identidad validada no puede presentar")
    void rechazaJuridicaSinAcreditacion() {
        cuenta.setTipoPersona(TipoPersona.JURIDICA);
        cuenta.setIdentidadValidadaOficialmente(false);
        when(borradorRepository.findByIdWithLock(borrador.getId())).thenReturn(Optional.of(borrador));
        when(borradorService.listarDocumentos(borrador.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.presentarBorrador(borrador.getId(), confirmacion))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vigencia de poder");
    }

    @Test
    @DisplayName("Una persona jurídica con vigencia de poder adjunta puede presentar")
    void aceptaJuridicaConVigenciaDePoder() {
        cuenta.setTipoPersona(TipoPersona.JURIDICA);
        borrador.getDocumentos().add(DocumentoBorrador.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumentoAdjunto.VIGENCIA_PODER)
                .nombreArchivo("vigencia_poder.pdf")
                .fechaCarga(LocalDateTime.now())
                .build());
        prepararPresentacionApta();

        service.presentarBorrador(borrador.getId(), confirmacion);

        verify(expedienteRepository).save(any(Expediente.class));
    }

    private void prepararPresentacionApta() {
        when(borradorRepository.findByIdWithLock(borrador.getId())).thenReturn(Optional.of(borrador));
        when(borradorService.validarRequisitos(borrador.getId())).thenReturn(ValidacionRequisitosDto.builder()
                .borradorId(borrador.getId()).aptoParaPresentacion(true)
                .camposFaltantes(List.of()).documentosFaltantes(List.of()).build());
        when(secuenciaTramiteService.generarSiguienteNumeroExpediente()).thenReturn("EXP-2026-00101");
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expedienteMapper.toDto(any(Expediente.class))).thenReturn(ExpedienteResponseDto.builder().numeroTramite("EXP-2026-00101").build());
    }

    private Actor ciudadano() {
        return new Actor(personaId, "42567891", "María Quispe Huamán", EnumSet.of(RolUsuario.ROLE_CIUDADANO));
    }
}
