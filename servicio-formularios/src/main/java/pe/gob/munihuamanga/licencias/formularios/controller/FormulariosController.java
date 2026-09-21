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
import pe.gob.munihuamanga.licencias.formularios.service.GeneradorDocumentoService;

@RestController
@RequestMapping("/api/formularios")
@RequiredArgsConstructor
@Tag(name = "Formularios", description = "Generación de formatos oficiales en PDF y firma digital")
public class FormulariosController {

    private final GeneradorDocumentoService generadorDocumentoService;

    @PostMapping("/declaracion-jurada")
    @Operation(summary = "Generar PDF oficial de Declaración Jurada para el administrado")
    public ResponseEntity<byte[]> generarDeclaracionJurada(@RequestBody ExpedienteResponseDto expediente) {
        byte[] pdf = generadorDocumentoService.generarDeclaracionJurada(expediente);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DeclaracionJurada-" + expediente.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
