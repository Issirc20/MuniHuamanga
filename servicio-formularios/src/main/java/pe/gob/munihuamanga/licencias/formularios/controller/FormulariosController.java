package pe.gob.munihuamanga.licencias.formularios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.formularios.service.GeneradorDocumentoService;

@RestController
@RequestMapping("/api/formularios")
@RequiredArgsConstructor
@Tag(name = "Formularios y Documentos", description = "Generación y descarga de formatos oficiales en PDF según la Ley N° 28976")
public class FormulariosController {

    private final GeneradorDocumentoService generadorDocumentoService;

    @PostMapping("/declaracion-jurada")
    @Operation(summary = "US-05: Generar PDF oficial de Declaración Jurada (Anexo 1 del D.S. N° 046-2017-PCM)")
    public ResponseEntity<byte[]> generarDeclaracionJurada(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = generadorDocumentoService.generarDeclaracionJurada(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DeclaracionJurada-" + expediente.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/defensa-civil")
    @Operation(summary = "US-05: Generar PDF oficial de Solicitud de Inspección Técnica ITSE (D.S. N° 002-2018-PCM)")
    public ResponseEntity<byte[]> generarSolicitudItse(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = generadorDocumentoService.generarSolicitudItsePdf(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SolicitudITSE-" + expediente.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/voucher-sat")
    @Operation(summary = "US-07: Generar PDF oficial del Voucher de Pago SAT con código de barras Code 128")
    public ResponseEntity<byte[]> generarVoucherSat(@RequestBody VoucherDto voucher) {
        byte[] pdf = generadorDocumentoService.generarVoucherSatPdf(voucher);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=VoucherSAT-" + voucher.getVoucherId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
