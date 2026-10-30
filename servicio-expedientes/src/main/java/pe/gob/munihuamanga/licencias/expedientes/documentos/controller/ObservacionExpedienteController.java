package pe.gob.munihuamanga.licencias.expedientes.documentos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.CrearObservacionDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservacionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.SubsanarObservacionesDto;
import pe.gob.munihuamanga.licencias.common.dto.SustentoDto;
import pe.gob.munihuamanga.licencias.expedientes.documentos.service.ObservacionExpedienteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expedientes/{id}/observaciones")
@RequiredArgsConstructor
@Tag(name = "Observaciones y subsanaciones", description = "Observaciones formales y su subsanación (TUO de la Ley N° 27444, art. 136)")
public class ObservacionExpedienteController {

    private final ObservacionExpedienteService observacionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MESA_DE_PARTES', 'EVALUADOR')")
    @Operation(summary = "Formular observaciones en la etapa de admisibilidad o durante la evaluación")
    public ResponseEntity<List<ObservacionExpedienteDto>> formularObservaciones(
            @PathVariable UUID id, @Valid @RequestBody List<@Valid CrearObservacionDto> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(observacionService.formularObservaciones(id, dtos));
    }

    @GetMapping
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar las observaciones del expediente y sus plazos de subsanación")
    public ResponseEntity<List<ObservacionExpedienteDto>> listarObservaciones(@PathVariable UUID id) {
        return ResponseEntity.ok(observacionService.listarObservaciones(id));
    }

    @PostMapping("/subsanar")
    @PreAuthorize("@expedienteSecurity.puedeSubsanar(authentication, #id)")
    @Operation(summary = "Presentar la subsanación de la totalidad de las observaciones")
    public ResponseEntity<Void> subsanarObservaciones(@PathVariable UUID id, @Valid @RequestBody SubsanarObservacionesDto dto) {
        observacionService.subsanarObservaciones(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reanudar-revision")
    @PreAuthorize("hasAnyRole('MESA_DE_PARTES', 'EVALUADOR')")
    @Operation(summary = "Verificar la subsanación y retornar el expediente a la etapa en que fue observado")
    public ResponseEntity<Void> reanudarRevision(@PathVariable UUID id, @Valid @RequestBody SustentoDto dto) {
        observacionService.reanudarRevision(id, dto.getSustento());
        return ResponseEntity.noContent().build();
    }
}
