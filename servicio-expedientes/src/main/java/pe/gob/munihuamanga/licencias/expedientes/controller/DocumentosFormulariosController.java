package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
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
@RequiredArgsConstructor
@Tag(name = "Formularios y Documentos Oficiales", description = "Generación y descarga de formatos oficiales en PDF según la Ley N° 28976 y D.S. N° 002-2018-PCM")
public class DocumentosFormulariosController {

    private final DocumentoPdfService documentoPdfService;
    private final ExpedienteService expedienteService;
    private final ExpedienteMapper expedienteMapper;

    @PostMapping("/anexo1-declaracion-jurada")
    @Operation(summary = "Generar PDF oficial del Anexo 1 de 2 páginas (Ley N° 28976 / D.S. N° 046-2017-PCM)")
    public ResponseEntity<byte[]> generarAnexo1DeclaracionJurada(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = documentoPdfService.generarAnexo1DeclaracionJurada(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo1-DeclaracionJurada-" + expediente.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/declaracion-jurada")
    @Operation(summary = "Generar PDF oficial de Declaración Jurada (Anexo 1)")
    public ResponseEntity<byte[]> generarDeclaracionJurada(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = documentoPdfService.generarDeclaracionJurada(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DeclaracionJurada-" + expediente.getNumeroTramite() + ".pdf")
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

    @PostMapping("/defensa-civil")
    @Operation(summary = "Generar PDF de Solicitud de Inspección Técnica ITSE")
    public ResponseEntity<byte[]> generarSolicitudItse(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = documentoPdfService.generarAnexo3MatrizRiesgoItse(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SolicitudITSE-" + expediente.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/voucher-sat")
    @Operation(summary = "Generar PDF oficial de Voucher de Pago SAT con código de barras")
    public ResponseEntity<byte[]> generarVoucherSat(@RequestBody VoucherDto voucher) {
        byte[] pdf = documentoPdfService.generarVoucherSatPdf(voucher);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=VoucherSAT-" + voucher.getVoucherId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/licencia")
    @Operation(summary = "Generar PDF oficial del Certificado de Licencia con QR")
    public ResponseEntity<byte[]> generarLicencia(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = documentoPdfService.generarLicenciaPdf(expediente);
        String codigo = expediente.getLicenciaQrCode() != null ? expediente.getLicenciaQrCode() : expediente.getNumeroTramite();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Licencia-" + codigo + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
