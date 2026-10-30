package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import pe.gob.munihuamanga.licencias.expedientes.service.ExpedienteService;

import java.util.UUID;

/**
 * Controlador unificado de Formularios Oficiales en PDF (Monolito Modular - Clean Architecture).
 * Soporta generación bajo demanda por DTO (POST) o recuperación por expediente (GET).
 */
@RestController
@RequestMapping("/api/formularios")
@Tag(name = "Formularios y Documentos Oficiales", description = "Generación y descarga de formatos oficiales en PDF según la Ley N° 28976 y D.S. N° 002-2018-PCM")
public class DocumentosFormulariosController {

    private final DocumentoPdfService documentoPdfService;
    private final ExpedienteService expedienteService;
    private final ExpedienteMapper expedienteMapper;

    public DocumentosFormulariosController(DocumentoPdfService documentoPdfService,
                                           ExpedienteService expedienteService,
                                           ExpedienteMapper expedienteMapper) {
        this.documentoPdfService = documentoPdfService;
        this.expedienteService = expedienteService;
        this.expedienteMapper = expedienteMapper;
    }

    @PostMapping({"/anexo1-declaracion-jurada", "/declaracion-jurada"})
    @Operation(summary = "Generar PDF oficial del Anexo 1 Licencia (Ley N° 28976 / D.S. N° 046-2017-PCM)")
    public ResponseEntity<byte[]> generarAnexo1DeclaracionJurada(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = documentoPdfService.generarAnexo1DeclaracionJurada(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo1-DeclaracionJurada-Licencia-" + expediente.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/anexo-1/{id}/pdf")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar PDF oficial de Anexo 1 Licencia por ID de expediente")
    public ResponseEntity<byte[]> descargarAnexo1PorId(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarAnexo1DeclaracionJurada(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo1-DeclaracionJurada-Licencia-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/anexo1-itse-solicitud")
    @Operation(summary = "Generar PDF oficial del Anexo 1 ITSE (Solicitud ITSE / ECSE)")
    public ResponseEntity<byte[]> generarAnexo1ItseSolicitud(@RequestBody pe.gob.munihuamanga.licencias.common.dto.Anexo1ItseDto dto) {
        String numTramite = dto.getNumeroExpediente() != null && !dto.getNumeroExpediente().isBlank()
                ? dto.getNumeroExpediente() : "EXP-BORRADOR";
        byte[] pdf = documentoPdfService.generarAnexo1ItseDirecto(dto, numTramite);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo1-ITSE-Solicitud-" + numTramite + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/anexo1-itse/{id}/pdf")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar PDF oficial de Anexo 1 ITSE por ID de expediente")
    public ResponseEntity<byte[]> descargarAnexo1ItsePorId(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarAnexo1Itse(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo1-ITSE-Solicitud-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/anexo2-itse-riesgo")
    @Operation(summary = "Generar PDF oficial del Anexo 2 ITSE (Información para determinación del nivel de riesgo)")
    public ResponseEntity<byte[]> generarAnexo2ItseRiesgo(@RequestBody pe.gob.munihuamanga.licencias.common.dto.Anexo2ItseDto dto) {
        String numTramite = "EXP-BORRADOR";
        byte[] pdf = documentoPdfService.generarAnexo2ItseDirecto(dto, numTramite, null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo2-ITSE-DeterminacionRiesgo-" + numTramite + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/anexo2-itse/{id}/pdf")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar PDF oficial de Anexo 2 ITSE por ID de expediente")
    public ResponseEntity<byte[]> descargarAnexo2ItsePorId(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarAnexo2Itse(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo2-ITSE-DeterminacionRiesgo-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/anexo3-matriz-riesgo-itse")
    @Operation(summary = "Generar PDF oficial del Anexo 3 de 2 páginas (Matriz de Riesgo ITSE según D.S. N° 002-2018-PCM)")
    public ResponseEntity<byte[]> generarAnexo3MatrizRiesgoItse(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = documentoPdfService.generarAnexo3MatrizRiesgoItse(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo3-MatrizRiesgoITSE-" + expediente.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/anexo-3/{id}/pdf")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar PDF oficial de Anexo 3 por ID de expediente")
    public ResponseEntity<byte[]> descargarAnexo3PorId(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarAnexo3MatrizRiesgoItse(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo3-MatrizRiesgoITSE-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/anexo4-condiciones-seguridad")
    @Operation(summary = "Generar PDF oficial del Anexo 4 de 4 páginas (Declaración Jurada de Cumplimiento de Condiciones de Seguridad)")
    public ResponseEntity<byte[]> generarAnexo4CondicionesSeguridad(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = documentoPdfService.generarAnexo4CondicionesSeguridad(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo4-CondicionesSeguridad-" + expediente.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/anexo-4/{id}/pdf")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar PDF oficial de Anexo 4 por ID de expediente")
    public ResponseEntity<byte[]> descargarAnexo4PorId(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarAnexo4CondicionesSeguridad(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo4-CondicionesSeguridad-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
