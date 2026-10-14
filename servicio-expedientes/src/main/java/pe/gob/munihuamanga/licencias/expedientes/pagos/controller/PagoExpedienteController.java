package pe.gob.munihuamanga.licencias.expedientes.pagos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.DeclararPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialDecisionPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.PagoExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.RechazarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarPagoManualDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.expedientes.pagos.service.PagoExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.security.CustomUserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pagos SAT y Validación Manual", description = "Recepción, revisión y validación manual de pagos de tasas TUPA (M05 - I06)")
public class PagoExpedienteController {

    private final PagoExpedienteService pagoService;

    @PostMapping("/api/expedientes/{id}/pagos/declarar")
    @Operation(summary = "Declarar comprobante de pago de tasa administrativa (Administrado)")
    public ResponseEntity<PagoExpedienteDto> declararPago(
            @PathVariable UUID id,
            @Valid @RequestBody DeclararPagoDto dto,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication != null ? authentication.getName() : null;
        String roles = extraerRoles(authentication);

        PagoExpedienteDto creado = pagoService.declararPago(id, dto, usuarioId, username, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/api/expedientes/{id}/pagos")
    @Operation(summary = "Listar comprobantes de pago registrados para un expediente")
    public ResponseEntity<List<PagoExpedienteDto>> listarPagosPorExpediente(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication != null ? authentication.getName() : null;
        String roles = extraerRoles(authentication);

        return ResponseEntity.ok(pagoService.listarPagosPorExpediente(id, usuarioId, username, roles));
    }

    @GetMapping("/api/pagos/{id}")
    @Operation(summary = "Consultar el detalle de un comprobante de pago")
    public ResponseEntity<PagoExpedienteDto> obtenerPagoPorId(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication != null ? authentication.getName() : null;
        String roles = extraerRoles(authentication);

        return ResponseEntity.ok(pagoService.obtenerPagoPorId(id, usuarioId, username, roles));
    }

    @GetMapping("/api/pagos/{id}/historial")
    @Operation(summary = "Consultar la trazabilidad y decisiones tomadas sobre un comprobante")
    public ResponseEntity<List<HistorialDecisionPagoDto>> obtenerHistorialPago(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication != null ? authentication.getName() : null;
        String roles = extraerRoles(authentication);

        return ResponseEntity.ok(pagoService.obtenerHistorialPago(id, usuarioId, username, roles));
    }

    @GetMapping(value = {"/api/pagos/bandeja", "/api/pagos/pendientes"})
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    @Operation(summary = "Bandeja administrativa de comprobantes de pago con filtros (CAJERO / ADMIN)")
    public ResponseEntity<List<PagoExpedienteDto>> bandejaPagos(
            @RequestParam(required = false) EstadoValidacionPago estado,
            @RequestParam(required = false) UUID expedienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta
    ) {
        return ResponseEntity.ok(pagoService.buscarPagosConFiltros(estado, expedienteId, fechaDesde, fechaHasta));
    }

    @PostMapping("/api/pagos/{id}/iniciar-revision")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    @Operation(summary = "Poner un comprobante en estado de revisión y cotejo manual (CAJERO / ADMIN)")
    public ResponseEntity<PagoExpedienteDto> iniciarRevision(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID cajeroId = extraerUsuarioId(authentication);
        String cajeroNombre = authentication != null ? authentication.getName() : "CAJERO";

        return ResponseEntity.ok(pagoService.iniciarRevision(id, cajeroId, cajeroNombre));
    }

    @PostMapping(value = {"/api/pagos/{id}/validar-manual", "/api/pagos/{id}/validar"})
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    @Operation(summary = "Registrar la validación manual de un pago conforme a la tasa TUPA (CAJERO / ADMIN)")
    public ResponseEntity<PagoExpedienteDto> validarPagoManual(
            @PathVariable UUID id,
            @Valid @RequestBody ValidarPagoManualDto dto,
            Authentication authentication
    ) {
        UUID cajeroId = extraerUsuarioId(authentication);
        String cajeroNombre = authentication != null ? authentication.getName() : "CAJERO";

        return ResponseEntity.ok(pagoService.validarPagoManual(id, dto, cajeroId, cajeroNombre));
    }

    @PostMapping("/api/pagos/{id}/observar")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    @Operation(summary = "Formular observación sobre un comprobante de pago (CAJERO / ADMIN)")
    public ResponseEntity<PagoExpedienteDto> observarPago(
            @PathVariable UUID id,
            @Valid @RequestBody ObservarPagoDto dto,
            Authentication authentication
    ) {
        UUID cajeroId = extraerUsuarioId(authentication);
        String cajeroNombre = authentication != null ? authentication.getName() : "CAJERO";

        return ResponseEntity.ok(pagoService.observarPago(id, dto, cajeroId, cajeroNombre));
    }

    @PostMapping("/api/pagos/{id}/rechazar")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    @Operation(summary = "Rechazar un comprobante de pago (CAJERO / ADMIN)")
    public ResponseEntity<PagoExpedienteDto> rechazarPago(
            @PathVariable UUID id,
            @Valid @RequestBody RechazarPagoDto dto,
            Authentication authentication
    ) {
        UUID cajeroId = extraerUsuarioId(authentication);
        String cajeroNombre = authentication != null ? authentication.getName() : "CAJERO";

        return ResponseEntity.ok(pagoService.rechazarPago(id, dto, cajeroId, cajeroNombre));
    }

    private UUID extraerUsuarioId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }

    private String extraerRoles(Authentication authentication) {
        if (authentication == null) return "";
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}
