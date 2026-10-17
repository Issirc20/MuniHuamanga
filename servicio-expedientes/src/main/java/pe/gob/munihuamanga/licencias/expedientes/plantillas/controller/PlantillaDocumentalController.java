package pe.gob.munihuamanga.licencias.expedientes.plantillas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.PlantillaDocumentalDto;
import pe.gob.munihuamanga.licencias.common.dto.SustituirPlantillaDto;
import pe.gob.munihuamanga.licencias.common.dto.VersionPlantillaDto;
import pe.gob.munihuamanga.licencias.expedientes.plantillas.service.PlantillaDocumentalService;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la consulta ciudadana y la administración oficial de plantillas documentales.
 */
@Slf4j
@RestController
@RequestMapping("/api/plantillas-documentales")
@RequiredArgsConstructor
@Tag(name = "Plantillas Documentales", description = "Gestión, versionado inmutable y descarga de plantillas oficiales")
public class PlantillaDocumentalController {

    private final PlantillaDocumentalService plantillaService;

    @Operation(summary = "Listar plantillas vigentes")
    @GetMapping
    public ResponseEntity<List<PlantillaDocumentalDto>> listarTodas() {
        return ResponseEntity.ok(plantillaService.listarPlantillas());
    }

    @Operation(summary = "Listar plantillas por grupo (ITSE o LICENCIA)")
    @GetMapping("/grupo/{grupo}")
    public ResponseEntity<List<PlantillaDocumentalDto>> listarPorGrupo(@PathVariable String grupo) {
        return ResponseEntity.ok(plantillaService.listarPlantillasPorGrupo(grupo));
    }

    @Operation(summary = "Consultar metadatos de una plantilla")
    @GetMapping("/{codigo}")
    public ResponseEntity<PlantillaDocumentalDto> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(plantillaService.obtenerPlantillaPorCodigo(codigo));
    }

    @Operation(summary = "Descargar plantilla vigente en formato PDF")
    @GetMapping("/{codigo}/descargar")
    public ResponseEntity<byte[]> descargarPlantillaVigente(@PathVariable String codigo) {
        byte[] pdf = plantillaService.obtenerContenidoPlantillaVigente(codigo);
        PlantillaDocumentalDto dto = plantillaService.obtenerPlantillaPorCodigo(codigo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", dto.getNombreArchivo());
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Consultar historial de versiones de una plantilla")
    @GetMapping("/{codigo}/versiones")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR', 'SUBGERENTE', 'INSPECTOR', 'EVALUADOR')")
    public ResponseEntity<List<VersionPlantillaDto>> listarHistorial(@PathVariable String codigo) {
        return ResponseEntity.ok(plantillaService.listarHistorialVersiones(codigo));
    }

    @Operation(summary = "Descargar una versión histórica específica de la plantilla")
    @GetMapping("/{codigo}/versiones/{version}/descargar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR', 'SUBGERENTE', 'INSPECTOR', 'EVALUADOR')")
    public ResponseEntity<byte[]> descargarVersionHistorica(@PathVariable String codigo, @PathVariable Integer version) {
        byte[] pdf = plantillaService.obtenerContenidoVersion(codigo, version);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", codigo.toLowerCase() + "_v" + version + ".pdf");
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Sustituir una plantilla oficial por una nueva versión (Admin)")
    @PostMapping("/{codigo}/sustituir")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR')")
    public ResponseEntity<PlantillaDocumentalDto> sustituirPlantilla(
            @PathVariable String codigo,
            @Valid @RequestBody SustituirPlantillaDto dto,
            Principal principal) {
        String usuario = principal != null ? principal.getName() : "ADMIN_SISTEMA";
        PlantillaDocumentalDto actualizada = plantillaService.sustituirPlantilla(codigo, dto, usuario);
        return ResponseEntity.ok(actualizada);
    }

    @Operation(summary = "Restaurar una versión previa mediante nueva versión controlada (Admin)")
    @PostMapping("/{codigo}/versiones/{version}/restaurar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR')")
    public ResponseEntity<PlantillaDocumentalDto> restaurarVersion(
            @PathVariable String codigo,
            @PathVariable Integer version,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal) {
        String usuario = principal != null ? principal.getName() : "ADMIN_SISTEMA";
        String motivo = body != null ? body.get("motivo") : "Restauración solicitada por administración";
        PlantillaDocumentalDto restaurada = plantillaService.restaurarVersion(codigo, version, motivo, usuario);
        return ResponseEntity.ok(restaurada);
    }
}
