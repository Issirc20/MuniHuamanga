package pe.gob.munihuamanga.licencias.expedientes.reclamaciones;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.dto.ReclamacionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ReclamarExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolverReclamacionDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoReclamacion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.reclamaciones.model.ReclamacionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.reclamaciones.repository.ReclamacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.reclamaciones.service.ReclamacionExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReclamacionExpedienteServiceTest {

    @Mock
    private ReclamacionExpedienteRepository reclamacionRepository;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private ReclamacionExpedienteService service;

    private Expediente expedienteHistorico;
    private Cuenta cuentaReclamante;

    @BeforeEach
    void setUp() {
        service = new ReclamacionExpedienteService(
                reclamacionRepository,
                expedienteRepository,
                cuentaRepository,
                auditoriaService
        );

        UUID expId = UUID.randomUUID();
        expedienteHistorico = Expediente.builder()
                .id(expId)
                .numeroTramite("EXP-2024-001234")
                .documentoIdentidad("42567891")
                .nombreTitular("María Quispe Huamán")
                .estado(EstadoExpediente.DOCUMENTOS_VALIDADOS)
                .cuentaId(null) // Histórico sin cuentaId
                .solicitanteId(null)
                .build();

        UUID cuentaId = UUID.randomUUID();
        cuentaReclamante = Cuenta.builder()
                .id(cuentaId)
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("42567891")
                .razonSocialONombre("María Quispe Huamán")
                .email("maria.quispe@test.pe")
                .estado(EstadoCuenta.ACTIVA)
                .emailVerificado(true)
                .build();
    }

    @Test
    @DisplayName("I04: Solicitar reclamación de titularidad para expediente histórico genera solicitud PENDIENTE")
    void solicitarReclamacion_Exitoso() {
        ReclamarExpedienteDto dto = ReclamarExpedienteDto.builder()
                .cuentaId(cuentaReclamante.getId())
                .evidenciaTitularidad("Copia de DNI N° 42567891 y cargo sellado de mesa de partes")
                .motivo("Vincular expediente tramitado presencialmente en 2024 a mi cuenta virtual")
                .build();

        when(expedienteRepository.findById(expedienteHistorico.getId())).thenReturn(Optional.of(expedienteHistorico));
        when(cuentaRepository.findById(cuentaReclamante.getId())).thenReturn(Optional.of(cuentaReclamante));
        when(reclamacionRepository.existsByExpedienteIdAndEstado(expedienteHistorico.getId(), EstadoReclamacion.PENDIENTE)).thenReturn(false);
        when(reclamacionRepository.save(any(ReclamacionExpediente.class))).thenAnswer(inv -> inv.getArgument(0));

        ReclamacionExpedienteDto resultado = service.solicitarReclamacion(expedienteHistorico.getId(), dto, "42567891");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoReclamacion.PENDIENTE);
        assertThat(resultado.getEvidenciaTitularidad()).contains("Copia de DNI");
        // El expediente NO se vincula automáticamente hasta la resolución
        assertThat(expedienteHistorico.getCuentaId()).isNull();
    }

    @Test
    @DisplayName("I04: Aprobación de reclamación por funcionario municipal vincula formalmente la cuenta")
    void resolverReclamacion_Aprobada_VinculaTitularidad() {
        UUID reclamacionId = UUID.randomUUID();
        ReclamacionExpediente reclamacion = ReclamacionExpediente.builder()
                .id(reclamacionId)
                .expediente(expedienteHistorico)
                .cuenta(cuentaReclamante)
                .solicitanteUsername("42567891")
                .estado(EstadoReclamacion.PENDIENTE)
                .evidenciaTitularidad("Cargo de mesa de partes")
                .fechaSolicitud(LocalDateTime.now())
                .build();

        ResolverReclamacionDto dto = ResolverReclamacionDto.builder()
                .aprobado(true)
                .motivoResolucion("Evidencia cotejada y validada con los archivos físicos de la Subgerencia")
                .build();

        when(reclamacionRepository.findById(reclamacionId)).thenReturn(Optional.of(reclamacion));
        when(reclamacionRepository.save(any(ReclamacionExpediente.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID evaluadorId = UUID.randomUUID();
        ReclamacionExpedienteDto resuelta = service.resolverReclamacion(
                reclamacionId,
                dto,
                evaluadorId,
                "evaluador_carlos"
        );

        assertThat(resuelta.getEstado()).isEqualTo(EstadoReclamacion.APROBADA);
        // Ahora sí se vincula formalmente la titularidad al expediente
        assertThat(expedienteHistorico.getCuentaId()).isEqualTo(cuentaReclamante.getId());
        assertThat(expedienteHistorico.getSolicitanteId()).isEqualTo(cuentaReclamante.getId());
        verify(expedienteRepository).save(expedienteHistorico);
        verify(auditoriaService).registrarTransicion(any(), any(), any(), eq("evaluador_carlos"), any());
    }

    @Test
    @DisplayName("I04: Rechazo de reclamación no altera la titularidad del expediente")
    void resolverReclamacion_Rechazada_NoAlteraTitularidad() {
        UUID reclamacionId = UUID.randomUUID();
        ReclamacionExpediente reclamacion = ReclamacionExpediente.builder()
                .id(reclamacionId)
                .expediente(expedienteHistorico)
                .cuenta(cuentaReclamante)
                .solicitanteUsername("42567891")
                .estado(EstadoReclamacion.PENDIENTE)
                .evidenciaTitularidad("Documento ilegible")
                .fechaSolicitud(LocalDateTime.now())
                .build();

        ResolverReclamacionDto dto = ResolverReclamacionDto.builder()
                .aprobado(false)
                .motivoResolucion("Documentación adjunta ilegible. No acredita titularidad del local")
                .build();

        when(reclamacionRepository.findById(reclamacionId)).thenReturn(Optional.of(reclamacion));
        when(reclamacionRepository.save(any(ReclamacionExpediente.class))).thenAnswer(inv -> inv.getArgument(0));

        ReclamacionExpedienteDto resuelta = service.resolverReclamacion(
                reclamacionId,
                dto,
                UUID.randomUUID(),
                "evaluador_carlos"
        );

        assertThat(resuelta.getEstado()).isEqualTo(EstadoReclamacion.RECHAZADA);
        assertThat(expedienteHistorico.getCuentaId()).isNull(); // Permanece nulo
    }
}
