package pe.gob.munihuamanga.licencias.expedientes.evaluacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarInformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.AprobarInformeDto;
import pe.gob.munihuamanga.licencias.common.dto.BandejaEvaluacionDto;
import pe.gob.munihuamanga.licencias.common.dto.ChecklistAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearInformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.FiltroBandejaEvaluacionDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialDecisionInformeDto;
import pe.gob.munihuamanga.licencias.common.dto.InformeAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.RevisarInformeDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.BandejaEvaluacionService;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.ChecklistAdministrativoService;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.InformeAdministrativoService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Evaluación Administrativa e Informes", description = "Endpoints de bandeja de evaluación, checklist integral y ciclo de vida de informes técnicos (M08)")
@RestController
@RequestMapping("/api/evaluaciones-administrativas")
@RequiredArgsConstructor
public class EvaluacionAdministrativaController {

    private final BandejaEvaluacionService bandejaEvaluacionService;
    private final ChecklistAdministrativoService checklistAdministrativoService;
    private final InformeAdministrativoService informeAdministrativoService;

    @Operation(summary = "Consultar bandeja unificada de evaluación con filtros y paginación")
    @GetMapping("/bandeja")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<Page<BandejaEvaluacionDto>> listarBandeja(
            @RequestParam(required = false) EstadoExpediente estadoExpediente,
            @RequestParam(required = false) EstadoValidacionPago estadoPago,
            @RequestParam(required = false) ResultadoZonificacion resultadoZonificacion,
            @RequestParam(required = false) NivelRiesgo nivelRiesgo,
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 20) Pageable pageable) {

        FiltroBandejaEvaluacionDto filtro = FiltroBandejaEvaluacionDto.builder()
                .estadoExpediente(estadoExpediente)
                .estadoPago(estadoPago)
                .resultadoZonificacion(resultadoZonificacion)
                .nivelRiesgo(nivelRiesgo)
                .busqueda(busqueda)
                .build();

        return ResponseEntity.ok(bandejaEvaluacionService.listarBandeja(filtro, pageable));
    }

    @Operation(summary = "Ejecutar checklist administrativo integral de requisitos para un expediente")
    @GetMapping("/expedientes/{expedienteId}/checklist")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<ChecklistAdministrativoDto> obtenerChecklist(@PathVariable UUID expedienteId) {
        return ResponseEntity.ok(checklistAdministrativoService.generarChecklist(expedienteId));
    }

    @Operation(summary = "Elaborar informe administrativo técnico en estado BORRADOR")
    @PostMapping("/informes")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<InformeAdministrativoDto> elaborarInforme(
            @Valid @RequestBody CrearInformeAdministrativoDto dto,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(informeAdministrativoService.elaborarInforme(dto, auth));
    }

    @Operation(summary = "Actualizar fundamentos y conclusión de informe en estado BORRADOR u OBSERVADO")
    @PutMapping("/informes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<InformeAdministrativoDto> actualizarInforme(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarInformeAdministrativoDto dto,
            Authentication auth) {
        return ResponseEntity.ok(informeAdministrativoService.actualizarBorrador(id, dto, auth));
    }

    @Operation(summary = "Enviar informe a revisión técnica interna")
    @PostMapping("/informes/{id}/enviar-revision")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<InformeAdministrativoDto> enviarARevision(
            @PathVariable UUID id,
            Authentication auth) {
        return ResponseEntity.ok(informeAdministrativoService.enviarARevision(id, auth));
    }

    @Operation(summary = "Revisar informe (VISAR técnicamente o DEVOLVER con observaciones)")
    @PostMapping("/informes/{id}/revisar")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<InformeAdministrativoDto> revisarInforme(
            @PathVariable UUID id,
            @Valid @RequestBody RevisarInformeDto dto,
            Authentication auth) {
        return ResponseEntity.ok(informeAdministrativoService.revisarInforme(id, dto, auth));
    }

    @Operation(summary = "Aprobar formalmente el informe administrativo (autoridad competente)")
    @PostMapping("/informes/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InformeAdministrativoDto> aprobarInforme(
            @PathVariable UUID id,
            @Valid @RequestBody AprobarInformeDto dto,
            Authentication auth) {
        return ResponseEntity.ok(informeAdministrativoService.aprobarInforme(id, dto, auth));
    }

    @Operation(summary = "Consultar el informe administrativo actual de un expediente")
    @GetMapping("/expedientes/{expedienteId}/informe-actual")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<InformeAdministrativoDto> obtenerInformeActual(@PathVariable UUID expedienteId) {
        return ResponseEntity.ok(informeAdministrativoService.obtenerInformeActualPorExpediente(expedienteId));
    }

    @Operation(summary = "Consultar informe por su identificador UUID")
    @GetMapping("/informes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<InformeAdministrativoDto> obtenerInformePorId(@PathVariable UUID id) {
        return ResponseEntity.ok(informeAdministrativoService.obtenerPorId(id));
    }

    @Operation(summary = "Consultar historial inmutable de decisiones del informe")
    @GetMapping("/informes/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<List<HistorialDecisionInformeDto>> listarHistorial(@PathVariable UUID id) {
        return ResponseEntity.ok(informeAdministrativoService.listarHistorial(id));
    }

    @Operation(summary = "Descargar PDF preliminar del informe con marca de borrador")
    @GetMapping("/informes/{id}/pdf-preliminar")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<byte[]> descargarPdfPreliminar(@PathVariable UUID id) {
        byte[] pdfBytes = informeAdministrativoService.descargarPdfPreliminar(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "informe-preliminar-" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
