package pe.gob.munihuamanga.licencias.expedientes.evaluacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pe.gob.munihuamanga.licencias.common.dto.AprobarInformeDto;
import pe.gob.munihuamanga.licencias.common.dto.ChecklistAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearInformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.InformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.RevisarInformeDto;
import pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoInforme;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.HistorialInformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.ChecklistAdministrativoService;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.InformeAdministrativoService;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.InformePdfGenerator;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.SecuenciaInformeService;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformeAdministrativoServiceTest {

    @Mock
    private InformeAdministrativoRepository informeRepository;

    @Mock
    private HistorialInformeAdministrativoRepository historialRepository;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private ChecklistAdministrativoService checklistService;

    @Mock
    private InformePdfGenerator informePdfGenerator;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SecuenciaInformeService secuenciaInformeService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InformeAdministrativoService informeService;

    private UUID expedienteId;
    private Expediente expediente;

    @BeforeEach
    void setUp() {
        expedienteId = UUID.randomUUID();
        expediente = Expediente.builder()
                .id(expedienteId)
                .numeroTramite("EXP-2026-00500")
                .estado(EstadoExpediente.PRESENTADO)
                .nombreTitular("Carlos Quispe")
                .documentoIdentidad("41998877")
                .build();
    }

    @Test
    @DisplayName("Elaborar informe con conclusión FAVORABLE y checklist apto -> Exitoso en BORRADOR")
    void elaborarInforme_checklistApto_favorableExitoso() {
        when(authentication.getName()).thenReturn("evaluador_muni");
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));

        ChecklistAdministrativoDto chk = ChecklistAdministrativoDto.builder()
                .aptoParaInformeFavorable(true)
                .resumen("Cumple todo")
                .items(Collections.emptyList())
                .build();
        when(checklistService.generarChecklist(expedienteId)).thenReturn(chk);

        when(informeRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)).thenReturn(Optional.empty());
        when(secuenciaInformeService.generarSiguienteNumeroInforme()).thenReturn("INF-2026-00001");

        when(informeRepository.save(any(InformeAdministrativo.class))).thenAnswer(i -> {
            InformeAdministrativo inf = i.getArgument(0);
            return inf;
        });

        CrearInformeAdministrativoDto dto = CrearInformeAdministrativoDto.builder()
                .expedienteId(expedienteId)
                .conclusion(ConclusionInforme.FAVORABLE)
                .fundamentosAdministrativos("Se verificaron los requisitos TUPA y normativos vigentes de Huamanga.")
                .referenciasDocumentales("Anexo 1 presentado, pago SAT validado, zonificación conforme.")
                .observaciones("Ninguna")
                .build();

        InformeAdministrativoDto res = informeService.elaborarInforme(dto, authentication);

        assertThat(res).isNotNull();
        assertThat(res.getEstado()).isEqualTo(EstadoInforme.BORRADOR);
        assertThat(res.getConclusion()).isEqualTo(ConclusionInforme.FAVORABLE);
        assertThat(res.getVersion()).isEqualTo(1);
        assertThat(res.getNumeroInforme()).startsWith("INF-");

        verify(historialRepository).save(any());
        verify(expedienteRepository).save(expediente);
        assertThat(expediente.getEstado()).isEqualTo(EstadoExpediente.EN_EVALUACION_FINAL);
    }

    @Test
    @DisplayName("Elaborar informe con conclusión FAVORABLE pero checklist NO apto -> Lanza excepción y bloquea")
    void elaborarInforme_checklistNoApto_favorableLanzaExcepcion() {
        when(authentication.getName()).thenReturn("evaluador_muni");
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));

        ChecklistAdministrativoDto chk = ChecklistAdministrativoDto.builder()
                .aptoParaInformeFavorable(false)
                .resumen("Falta pago SAT")
                .build();
        when(checklistService.generarChecklist(expedienteId)).thenReturn(chk);

        CrearInformeAdministrativoDto dto = CrearInformeAdministrativoDto.builder()
                .expedienteId(expedienteId)
                .conclusion(ConclusionInforme.FAVORABLE)
                .fundamentosAdministrativos("Fundamentos completos con más de veinte caracteres.")
                .referenciasDocumentales("Referencias documentales.")
                .build();

        assertThatThrownBy(() -> informeService.elaborarInforme(dto, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede emitir conclusión FAVORABLE");
    }

    @Test
    @DisplayName("Enviar informe a revisión -> Cambia estado a EN_REVISION")
    void enviarARevision_cambiaEstadoEnRevision() {
        UUID informeId = UUID.randomUUID();
        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(informeId)
                .expediente(expediente)
                .estado(EstadoInforme.BORRADOR)
                .numeroInforme("INF-2026-00001")
                .version(1)
                .build();

        when(authentication.getName()).thenReturn("evaluador_muni");
        when(informeRepository.findById(informeId)).thenReturn(Optional.of(inf));
        when(informeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InformeAdministrativoDto res = informeService.enviarARevision(informeId, authentication);

        assertThat(res.getEstado()).isEqualTo(EstadoInforme.EN_REVISION);
        verify(historialRepository).save(any());
    }

    @Test
    @DisplayName("Revisar informe con acción VISAR -> Cambia a estado VISADO")
    void revisarInforme_visar_cambiaEstadoVisado() {
        UUID informeId = UUID.randomUUID();
        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(informeId)
                .expediente(expediente)
                .estado(EstadoInforme.EN_REVISION)
                .numeroInforme("INF-2026-00001")
                .version(1)
                .build();

        when(authentication.getName()).thenReturn("revisor_tecnico");
        when(informeRepository.findById(informeId)).thenReturn(Optional.of(inf));
        when(informeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RevisarInformeDto dto = RevisarInformeDto.builder()
                .accion("VISAR")
                .observacionesRevision("Conforme con la revisión técnica documental.")
                .build();

        InformeAdministrativoDto res = informeService.revisarInforme(informeId, dto, authentication);

        assertThat(res.getEstado()).isEqualTo(EstadoInforme.VISADO);
        assertThat(res.getRevisorNombre()).isEqualTo("revisor_tecnico");
    }

    @Test
    @DisplayName("Aprobar informe por el mismo funcionario evaluador -> Lanza excepción por segregación de funciones")
    void aprobarInforme_mismoEvaluador_lanzaExcepcion() {
        UUID informeId = UUID.randomUUID();
        UUID mismoId = UUID.nameUUIDFromBytes("funcionario_juan".getBytes());

        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(informeId)
                .expediente(expediente)
                .evaluadorId(mismoId)
                .evaluadorNombre("funcionario_juan")
                .estado(EstadoInforme.VISADO)
                .conclusion(ConclusionInforme.FAVORABLE)
                .build();

        when(authentication.getName()).thenReturn("funcionario_juan");
        when(informeRepository.findByIdWithLock(informeId)).thenReturn(Optional.of(inf));

        AprobarInformeDto dto = AprobarInformeDto.builder()
                .observacionesAprobacion("Aprobado formalmente por mí mismo.")
                .build();

        assertThatThrownBy(() -> informeService.aprobarInforme(informeId, dto, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Segregación de funciones");
    }

    @Test
    @DisplayName("Aprobar informe por admin_master que fue el mismo evaluador -> Lanza excepción (sin bypass)")
    void aprobarInforme_adminMasterMismoEvaluador_lanzaExcepcion() {
        UUID informeId = UUID.randomUUID();
        UUID adminId = UUID.nameUUIDFromBytes("admin_master".getBytes());

        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(informeId)
                .expediente(expediente)
                .evaluadorId(adminId)
                .evaluadorNombre("admin_master")
                .estado(EstadoInforme.VISADO)
                .conclusion(ConclusionInforme.FAVORABLE)
                .build();

        when(authentication.getName()).thenReturn("admin_master");
        when(informeRepository.findByIdWithLock(informeId)).thenReturn(Optional.of(inf));

        AprobarInformeDto dto = AprobarInformeDto.builder()
                .observacionesAprobacion("Aprobado por admin_master.")
                .build();

        assertThatThrownBy(() -> informeService.aprobarInforme(informeId, dto, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Segregación de funciones");
    }

    @Test
    @DisplayName("Aprobar informe en estado EN_REVISION sin haber sido visado -> Lanza excepción")
    void aprobarInforme_estadoEnRevisionSinVisado_lanzaExcepcion() {
        UUID informeId = UUID.randomUUID();
        UUID evaluadorId = UUID.nameUUIDFromBytes("evaluador".getBytes());

        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(informeId)
                .expediente(expediente)
                .evaluadorId(evaluadorId)
                .evaluadorNombre("evaluador")
                .estado(EstadoInforme.EN_REVISION)
                .conclusion(ConclusionInforme.FAVORABLE)
                .build();

        when(authentication.getName()).thenReturn("revisor_autoridad");
        when(informeRepository.findByIdWithLock(informeId)).thenReturn(Optional.of(inf));

        AprobarInformeDto dto = AprobarInformeDto.builder()
                .observacionesAprobacion("Aprobación intentada sin visado.")
                .build();

        assertThatThrownBy(() -> informeService.aprobarInforme(informeId, dto, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("debe estar debidamente VISADO");
    }

    @Test
    @DisplayName("Aprobar informe favorable por autoridad competente -> Expediente pasa a APTO_PARA_RESOLUCION")
    void aprobarInforme_favorableAprobado_transicionaExpedienteAptoParaResolucion() {
        UUID informeId = UUID.randomUUID();
        UUID evaluadorId = UUID.nameUUIDFromBytes("evaluador_muni".getBytes());

        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(informeId)
                .expediente(expediente)
                .evaluadorId(evaluadorId)
                .evaluadorNombre("evaluador_muni")
                .numeroInforme("INF-2026-00001")
                .version(1)
                .estado(EstadoInforme.VISADO)
                .conclusion(ConclusionInforme.FAVORABLE)
                .build();

        when(authentication.getName()).thenReturn("gerente_licencias");
        when(informeRepository.findByIdWithLock(informeId)).thenReturn(Optional.of(inf));

        ChecklistAdministrativoDto chk = ChecklistAdministrativoDto.builder()
                .aptoParaInformeFavorable(true)
                .build();
        when(checklistService.generarChecklist(expedienteId)).thenReturn(chk);

        when(informeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AprobarInformeDto dto = AprobarInformeDto.builder()
                .observacionesAprobacion("Aprobación del informe técnico para habilitar emisión de resolución.")
                .build();

        InformeAdministrativoDto res = informeService.aprobarInforme(informeId, dto, authentication);

        assertThat(res.getEstado()).isEqualTo(EstadoInforme.APROBADO);
        assertThat(res.getAprobadorNombre()).isEqualTo("gerente_licencias");

        verify(expedienteRepository).save(expediente);
        assertThat(expediente.getEstado()).isEqualTo(EstadoExpediente.APTO_PARA_RESOLUCION);
        verify(auditoriaService).registrarTransicion(eq(expedienteId), any(), eq(EstadoExpediente.APTO_PARA_RESOLUCION), eq("gerente_licencias"), any());
    }

    @Test
    @DisplayName("Aprobar informe ya aprobado -> Lanza excepción evitando doble aprobación")
    void aprobarInforme_yaAprobado_lanzaExcepcion() {
        UUID informeId = UUID.randomUUID();
        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(informeId)
                .estado(EstadoInforme.APROBADO)
                .build();

        when(informeRepository.findByIdWithLock(informeId)).thenReturn(Optional.of(inf));

        AprobarInformeDto dto = AprobarInformeDto.builder()
                .observacionesAprobacion("Re-aprobación indebida.")
                .build();

        assertThatThrownBy(() -> informeService.aprobarInforme(informeId, dto, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra aprobado");
    }

    @Test
    @DisplayName("Descargar PDF preliminar -> Retorna bytes válidos")
    void descargarPdfPreliminar_retornaBytes() {
        UUID informeId = UUID.randomUUID();
        InformeAdministrativo inf = InformeAdministrativo.builder()
                .id(informeId)
                .expediente(expediente)
                .numeroInforme("INF-2026-00001")
                .version(1)
                .estado(EstadoInforme.BORRADOR)
                .conclusion(ConclusionInforme.FAVORABLE)
                .evaluadorNombre("evaluador")
                .fechaElaboracion(LocalDateTime.now())
                .fundamentosAdministrativos("Fundamentos de prueba")
                .referenciasDocumentales("Referencias de prueba")
                .build();

        when(informeRepository.findById(informeId)).thenReturn(Optional.of(inf));
        when(checklistService.generarChecklist(expedienteId)).thenReturn(ChecklistAdministrativoDto.builder().build());
        when(informePdfGenerator.generarPdf(eq(inf), any())).thenReturn(new byte[]{1, 2, 3});

        byte[] pdf = informeService.descargarPdfPreliminar(informeId);

        assertThat(pdf).isNotEmpty();
        assertThat(pdf).containsExactly(1, 2, 3);
    }
}
