package pe.gob.munihuamanga.licencias.expedientes.establecimientos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.CatalogoCiiuDto;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.service.EstablecimientoService;

import java.util.List;

/**
 * Controlador REST para el Catálogo Oficial CIIU de Huamanga.
 */
@RestController
@RequestMapping("/api/catalogo-ciiu")
@RequiredArgsConstructor
@Tag(name = "Catálogo CIIU", description = "Consulta del catálogo oficial de actividades económicas y giros")
public class CatalogoCiiuController {

    private final EstablecimientoService establecimientoService;

    @GetMapping
    @Operation(summary = "Listar códigos CIIU activos con descripciones y giros oficiales")
    public ResponseEntity<List<CatalogoCiiuDto>> listarCatalogoCiiu() {
        return ResponseEntity.ok(establecimientoService.listarCatalogoCiiu());
    }
}
