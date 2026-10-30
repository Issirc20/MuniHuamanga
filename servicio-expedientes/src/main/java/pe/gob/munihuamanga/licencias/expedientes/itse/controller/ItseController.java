package pe.gob.munihuamanga.licencias.expedientes.itse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteItseDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservarItseDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistrarCargaItseDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarItseDto;
import pe.gob.munihuamanga.licencias.expedientes.itse.service.ItseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Etapa ITSE", description = "Carga y verificación de los anexos ITSE firmados por Defensa Civil")
public class ItseController {

    private final ItseService itseService;

    @GetMapping("/api/expedientes/{id}/itse")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar la situación de la etapa ITSE, sus anexos, observaciones y liquidación")
    public ResponseEntity<ExpedienteItseDto> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(itseService.obtener(id));
    }

    @PostMapping("/api/expedientes/{id}/itse/carga")
    @PreAuthorize("@expedienteSecurity.puedeActuarComoAdministrado(authentication, #id, 'PRESENTAR_EXPEDIENTE')")
    @Operation(summary = "Enviar a revisión los cuatro anexos ITSE firmados, previamente adjuntados al expediente")
    public ResponseEntity<ExpedienteItseDto> registrarCarga(@PathVariable UUID id, @Valid @RequestBody RegistrarCargaItseDto dto) {
        return ResponseEntity.ok(itseService.registrarCarga(id, dto));
    }

    @PostMapping("/api/expedientes/{id}/itse/observaciones")
    @PreAuthorize("hasRole('EVALUADOR')")
    @Operation(summary = "Observar los anexos ITSE cargados")
    public ResponseEntity<ExpedienteItseDto> observar(@PathVariable UUID id, @Valid @RequestBody ObservarItseDto dto) {
        return ResponseEntity.ok(itseService.observar(id, dto));
    }

    @PostMapping("/api/expedientes/{id}/itse/validacion")
    @PreAuthorize("hasRole('EVALUADOR')")
    @Operation(summary = "Validar los anexos ITSE, fijar el nivel de riesgo oficial y emitir la liquidación definitiva")
    public ResponseEntity<ExpedienteItseDto> validar(@PathVariable UUID id, @Valid @RequestBody ValidarItseDto dto) {
        return ResponseEntity.ok(itseService.validar(id, dto));
    }

    @GetMapping("/api/itse/bandeja")
    @PreAuthorize("hasAnyRole('EVALUADOR', 'SUPERVISOR_LICENCIAS', 'AUDITOR')")
    @Operation(summary = "Bandeja de anexos ITSE pendientes de revisión")
    public ResponseEntity<List<ExpedienteItseDto>> bandeja() {
        return ResponseEntity.ok(itseService.bandejaRevision());
    }
}
