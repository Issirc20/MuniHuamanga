package pe.gob.munihuamanga.licencias.expedientes.pagos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.DeclararPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialDecisionPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.PagoExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.RechazarPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarPagoManualDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.pagos.service.PagoExpedienteService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Declaración del comprobante por el administrado y validación por caja del SAT")
public class PagoExpedienteController {

    private final PagoExpedienteService pagoService;
    private final ProveedorActor proveedorActor;

    @PostMapping("/api/expedientes/{id}/pagos/declarar")
    @PreAuthorize("@expedienteSecurity.puedeDeclararPago(authentication, #id)")
    @Operation(summary = "Declarar el comprobante de pago de la tasa liquidada")
    public ResponseEntity<PagoExpedienteDto> declararPago(@PathVariable UUID id, @Valid @RequestBody DeclararPagoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.declararPago(id, dto));
    }

    @GetMapping("/api/expedientes/{id}/pagos")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Listar los comprobantes de pago del expediente")
    public ResponseEntity<List<PagoExpedienteDto>> listarPagosPorExpediente(@PathVariable UUID id) {
        return ResponseEntity.ok(pagoService.listarPagosPorExpediente(id));
    }

    @GetMapping("/api/pagos/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar el detalle de un comprobante de pago")
    public ResponseEntity<PagoExpedienteDto> obtenerPagoPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pagoService.obtenerPagoPorId(id));
    }

    @GetMapping("/api/pagos/{id}/historial")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar la trazabilidad de las decisiones sobre un comprobante")
    public ResponseEntity<List<HistorialDecisionPagoDto>> obtenerHistorialPago(@PathVariable UUID id) {
        return ResponseEntity.ok(pagoService.obtenerHistorialPago(id));
    }

    @GetMapping("/api/pagos/bandeja")
    @PreAuthorize("hasAnyRole('CAJERO', 'AUDITOR')")
    @Operation(summary = "Bandeja de comprobantes de pago con filtros")
    public ResponseEntity<List<PagoExpedienteDto>> bandejaPagos(
            @RequestParam(required = false) EstadoValidacionPago estado,
            @RequestParam(required = false) UUID expedienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return ResponseEntity.ok(pagoService.buscarPagosConFiltros(estado, expedienteId, fechaDesde, fechaHasta));
    }

    @PostMapping("/api/pagos/{id}/iniciar-revision")
    @PreAuthorize("hasRole('CAJERO')")
    @Operation(summary = "Tomar un comprobante en revisión")
    public ResponseEntity<PagoExpedienteDto> iniciarRevision(@PathVariable UUID id) {
        Actor cajero = proveedorActor.actual();
        return ResponseEntity.ok(pagoService.iniciarRevision(id, cajero.usuarioId(), cajero.nombreParaRegistro()));
    }

    @PostMapping("/api/pagos/{id}/validar")
    @PreAuthorize("hasRole('CAJERO')")
    @Operation(summary = "Validar el comprobante de pago frente a la tasa liquidada")
    public ResponseEntity<PagoExpedienteDto> validarPagoManual(@PathVariable UUID id, @Valid @RequestBody ValidarPagoManualDto dto) {
        Actor cajero = proveedorActor.actual();
        return ResponseEntity.ok(pagoService.validarPagoManual(id, dto, cajero.usuarioId(), cajero.nombreParaRegistro()));
    }

    @PostMapping("/api/pagos/{id}/observar")
    @PreAuthorize("hasRole('CAJERO')")
    @Operation(summary = "Observar un comprobante de pago")
    public ResponseEntity<PagoExpedienteDto> observarPago(@PathVariable UUID id, @Valid @RequestBody ObservarPagoDto dto) {
        Actor cajero = proveedorActor.actual();
        return ResponseEntity.ok(pagoService.observarPago(id, dto, cajero.usuarioId(), cajero.nombreParaRegistro()));
    }

    @PostMapping("/api/pagos/{id}/rechazar")
    @PreAuthorize("hasRole('CAJERO')")
    @Operation(summary = "Rechazar un comprobante de pago")
    public ResponseEntity<PagoExpedienteDto> rechazarPago(@PathVariable UUID id, @Valid @RequestBody RechazarPagoDto dto) {
        Actor cajero = proveedorActor.actual();
        return ResponseEntity.ok(pagoService.rechazarPago(id, dto, cajero.usuarioId(), cajero.nombreParaRegistro()));
    }
}
