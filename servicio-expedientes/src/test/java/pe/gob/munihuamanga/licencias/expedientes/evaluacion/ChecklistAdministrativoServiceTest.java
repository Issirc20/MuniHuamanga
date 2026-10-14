package pe.gob.munihuamanga.licencias.expedientes.evaluacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.dto.ChecklistAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.ItemChecklistDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoChecklist;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.ObservacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.ChecklistAdministrativoService;
import pe.gob.munihuamanga.licencias.expedientes.model.Anexo4Condiciones;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.PagoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.PagoExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.model.EvaluacionZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.repository.EvaluacionZonificacionRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChecklistAdministrativoServiceTest {

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private DocumentoVersionadoRepository documentoVersionadoRepository;

    @Mock
    private ObservacionExpedienteRepository observacionExpedienteRepository;

    @Mock
    private PagoExpedienteRepository pagoExpedienteRepository;

    @Mock
    private EvaluacionZonificacionRepository evaluacionZonificacionRepository;

    @Mock
    private EstablecimientoRepository establecimientoRepository;

    @InjectMocks
    private ChecklistAdministrativoService checklistService;

    private UUID expedienteId;
    private UUID establecimientoId;
    private Expediente expediente;

    @BeforeEach
    void setUp() {
        expedienteId = UUID.randomUUID();
        establecimientoId = UUID.randomUUID();

        expediente = Expediente.builder()
                .id(expedienteId)
                .numeroTramite("EXP-2026-00100")
                .estado(EstadoExpediente.PRESENTADO)
                .nombreTitular("Juan Pérez Huamán")
                .documentoIdentidad("42889911")
                .establecimientoId(establecimientoId)
                .nombreComercial("Botica San Juan")
                .direccionEstablecimiento("Jr. Asamblea 123")
                .nivelRiesgo(NivelRiesgo.BAJO)
                .montoTasa(new BigDecimal("154.50"))
                .anexo4Condiciones(new Anexo4Condiciones())
                .build();
    }

    @Test
    @DisplayName("Checklist completo cumple todos los requisitos -> Apto para informe favorable")
    void generarChecklist_todosRequisitosCumplidos_aptoFavorable() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.existsById(establecimientoId)).thenReturn(true);

        DocumentoVersionado docAnexo1 = DocumentoVersionado.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();
        when(documentoVersionadoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId))
                .thenReturn(List.of(docAnexo1));

        when(observacionExpedienteRepository.countByExpedienteIdAndSubsanadaFalse(expedienteId)).thenReturn(0L);

        PagoExpediente pago = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .estado(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .validadorNombre("Cajero SAT")
                .numeroOperacionSat("OP-2026-1122")
                .build();
        when(pagoExpedienteRepository.findByExpedienteIdOrderByFechaRegistroDesc(expedienteId))
                .thenReturn(List.of(pago));

        EvaluacionZonificacion zon = EvaluacionZonificacion.builder()
                .id(UUID.randomUUID())
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .fuenteSustentoTecnico("Informe Técnico N° 088-2026-MPH")
                .build();
        when(evaluacionZonificacionRepository.findFirstByExpedienteIdOrderByFechaRegistroDesc(expedienteId))
                .thenReturn(Optional.of(zon));

        ChecklistAdministrativoDto resultado = checklistService.generarChecklist(expedienteId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isAptoParaInformeFavorable()).isTrue();
        assertThat(resultado.getItems()).hasSize(9);
        assertThat(resultado.getItems()).allMatch(i -> i.getEstado() == EstadoChecklist.CUMPLE || i.getEstado() == EstadoChecklist.NO_APLICA);
    }

    @Test
    @DisplayName("Checklist con pago pendiente -> NO CUMPLE en CHK-06 y no apto para informe favorable")
    void generarChecklist_pagoPendiente_noAptoFavorable() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.existsById(establecimientoId)).thenReturn(true);

        DocumentoVersionado doc = DocumentoVersionado.builder()
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();
        when(documentoVersionadoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)).thenReturn(List.of(doc));
        when(observacionExpedienteRepository.countByExpedienteIdAndSubsanadaFalse(expedienteId)).thenReturn(0L);

        PagoExpediente pago = PagoExpediente.builder()
                .id(UUID.randomUUID())
                .estado(EstadoValidacionPago.PENDIENTE_VALIDACION)
                .build();
        when(pagoExpedienteRepository.findByExpedienteIdOrderByFechaRegistroDesc(expedienteId)).thenReturn(List.of(pago));

        EvaluacionZonificacion zon = EvaluacionZonificacion.builder()
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .fuenteSustentoTecnico("Informe N° 01")
                .build();
        when(evaluacionZonificacionRepository.findFirstByExpedienteIdOrderByFechaRegistroDesc(expedienteId)).thenReturn(Optional.of(zon));

        ChecklistAdministrativoDto resultado = checklistService.generarChecklist(expedienteId);

        assertThat(resultado.isAptoParaInformeFavorable()).isFalse();
        ItemChecklistDto itemPago = resultado.getItems().stream()
                .filter(i -> i.getCodigo().equals("CHK-06-PAGO_SAT"))
                .findFirst().orElseThrow();
        assertThat(itemPago.getEstado()).isEqualTo(EstadoChecklist.NO_CUMPLE);
    }

    @Test
    @DisplayName("Checklist con zonificación NO_COMPATIBLE -> NO CUMPLE en CHK-07")
    void generarChecklist_zonificacionNoCompatible_noAptoFavorable() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.existsById(establecimientoId)).thenReturn(true);

        DocumentoVersionado doc = DocumentoVersionado.builder()
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();
        when(documentoVersionadoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)).thenReturn(List.of(doc));
        when(observacionExpedienteRepository.countByExpedienteIdAndSubsanadaFalse(expedienteId)).thenReturn(0L);

        PagoExpediente pago = PagoExpediente.builder()
                .estado(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .validadorNombre("Cajero")
                .numeroOperacionSat("OP-100")
                .build();
        when(pagoExpedienteRepository.findByExpedienteIdOrderByFechaRegistroDesc(expedienteId)).thenReturn(List.of(pago));

        EvaluacionZonificacion zon = EvaluacionZonificacion.builder()
                .resultado(ResultadoZonificacion.NO_COMPATIBLE)
                .fuenteSustentoTecnico("Informe Negativo N° 02")
                .build();
        when(evaluacionZonificacionRepository.findFirstByExpedienteIdOrderByFechaRegistroDesc(expedienteId)).thenReturn(Optional.of(zon));

        ChecklistAdministrativoDto resultado = checklistService.generarChecklist(expedienteId);

        assertThat(resultado.isAptoParaInformeFavorable()).isFalse();
        ItemChecklistDto itemZon = resultado.getItems().stream()
                .filter(i -> i.getCodigo().equals("CHK-07-ZONIFICACION"))
                .findFirst().orElseThrow();
        assertThat(itemZon.getEstado()).isEqualTo(EstadoChecklist.NO_CUMPLE);
    }

    @Test
    @DisplayName("Checklist con observaciones pendientes -> NO CUMPLE en CHK-05")
    void generarChecklist_observacionesPendientes_noAptoFavorable() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.existsById(establecimientoId)).thenReturn(true);

        DocumentoVersionado doc = DocumentoVersionado.builder()
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();
        when(documentoVersionadoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId)).thenReturn(List.of(doc));

        // 2 observaciones sin subsanar
        when(observacionExpedienteRepository.countByExpedienteIdAndSubsanadaFalse(expedienteId)).thenReturn(2L);

        PagoExpediente pago = PagoExpediente.builder()
                .estado(EstadoValidacionPago.VALIDADO_MANUALMENTE)
                .validadorNombre("Cajero")
                .numeroOperacionSat("OP-100")
                .build();
        when(pagoExpedienteRepository.findByExpedienteIdOrderByFechaRegistroDesc(expedienteId)).thenReturn(List.of(pago));

        EvaluacionZonificacion zon = EvaluacionZonificacion.builder()
                .resultado(ResultadoZonificacion.COMPATIBLE)
                .fuenteSustentoTecnico("Informe N° 01")
                .build();
        when(evaluacionZonificacionRepository.findFirstByExpedienteIdOrderByFechaRegistroDesc(expedienteId)).thenReturn(Optional.of(zon));

        ChecklistAdministrativoDto resultado = checklistService.generarChecklist(expedienteId);

        assertThat(resultado.isAptoParaInformeFavorable()).isFalse();
        ItemChecklistDto itemObs = resultado.getItems().stream()
                .filter(i -> i.getCodigo().equals("CHK-05-OBSERVACIONES"))
                .findFirst().orElseThrow();
        assertThat(itemObs.getEstado()).isEqualTo(EstadoChecklist.NO_CUMPLE);
    }

    @Test
    @DisplayName("Persona jurídica sin vigencia de poder ni datos SUNARP -> NO CUMPLE en CHK-03")
    void generarChecklist_personaJuridicaSinVigenciaPoder_noCumpleChk03() {
        expediente.setTipoPersona(TipoPersona.JURIDICA);
        expediente.setPartidaSunarp(null);
        expediente.setPoderSunarp(null);

        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.existsById(establecimientoId)).thenReturn(true);

        DocumentoVersionado docAnexo1 = DocumentoVersionado.builder()
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();
        when(documentoVersionadoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId))
                .thenReturn(List.of(docAnexo1));

        ChecklistAdministrativoDto resultado = checklistService.generarChecklist(expedienteId);

        assertThat(resultado.isAptoParaInformeFavorable()).isFalse();
        ItemChecklistDto itemDocs = resultado.getItems().stream()
                .filter(i -> i.getCodigo().equals("CHK-03-DOCUMENTOS"))
                .findFirst().orElseThrow();
        assertThat(itemDocs.getEstado()).isEqualTo(EstadoChecklist.NO_CUMPLE);
        assertThat(itemDocs.getMotivo()).contains("Persona jurídica sin acreditar vigencia de poder");
    }

    @Test
    @DisplayName("Documento vigente en estado OBSERVADO -> NO CUMPLE en CHK-04")
    void generarChecklist_documentoObservado_noCumpleChk04() {
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.existsById(establecimientoId)).thenReturn(true);

        DocumentoVersionado docObs = DocumentoVersionado.builder()
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .estadoRevision(EstadoRevisionDocumento.OBSERVADO)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();
        when(documentoVersionadoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId))
                .thenReturn(List.of(docObs));

        ChecklistAdministrativoDto resultado = checklistService.generarChecklist(expedienteId);

        assertThat(resultado.isAptoParaInformeFavorable()).isFalse();
        ItemChecklistDto itemVersiones = resultado.getItems().stream()
                .filter(i -> i.getCodigo().equals("CHK-04-VERSIONES"))
                .findFirst().orElseThrow();
        assertThat(itemVersiones.getEstado()).isEqualTo(EstadoChecklist.NO_CUMPLE);
        assertThat(itemVersiones.getMotivo()).contains("OBSERVADO");
    }

    @Test
    @DisplayName("Riesgo ALTO sin planos ni documentos técnicos ITSE previa -> NO CUMPLE en CHK-08")
    void generarChecklist_riesgoAltoSinPlanosItsePrevia_noCumpleChk08() {
        expediente.setNivelRiesgo(NivelRiesgo.ALTO);

        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(establecimientoRepository.existsById(establecimientoId)).thenReturn(true);

        DocumentoVersionado docAnexo1 = DocumentoVersionado.builder()
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .esVersionActual(true)
                .hashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();
        // Solo anexo 1, ningún plano técnico o plan de seguridad
        when(documentoVersionadoRepository.findByExpedienteIdAndEsVersionActualTrue(expedienteId))
                .thenReturn(List.of(docAnexo1));

        ChecklistAdministrativoDto resultado = checklistService.generarChecklist(expedienteId);

        assertThat(resultado.isAptoParaInformeFavorable()).isFalse();
        ItemChecklistDto itemItse = resultado.getItems().stream()
                .filter(i -> i.getCodigo().equals("CHK-08-SUSTENTO_ITSE"))
                .findFirst().orElseThrow();
        assertThat(itemItse.getEstado()).isEqualTo(EstadoChecklist.NO_CUMPLE);
        assertThat(itemItse.getMotivo()).contains("ITSE Previa");
    }
}
