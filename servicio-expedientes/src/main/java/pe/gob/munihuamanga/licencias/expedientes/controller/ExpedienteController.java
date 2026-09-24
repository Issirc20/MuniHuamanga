package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ClasificacionRiesgoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialEstadoDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolucionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionLicenciaDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.service.CalculadoraDeTasa;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import pe.gob.munihuamanga.licencias.expedientes.service.ExpedienteService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para la gestión integral del trámite de licencias de funcionamiento.
 */
@RestController
@RequestMapping("/api/expedientes")
@RequiredArgsConstructor
@Tag(name = "Expedientes", description = "Endpoints para la gestión, seguimiento, formatos PDF y transiciones del trámite")
public class ExpedienteController {

    private final ExpedienteService expedienteService;
    private final ExpedienteMapper expedienteMapper;
    private final AuditoriaService auditoriaService;
    private final CalculadoraDeTasa calculadoraDeTasa;
    private final DocumentoPdfService documentoPdfService;

    @PostMapping
    @Operation(summary = "Mesa de Partes Virtual: Registrar nueva solicitud de licencia")
    public ResponseEntity<ExpedienteResponseDto> crearExpediente(@Valid @RequestBody CrearExpedienteDto dto) {
        Expediente nuevo = expedienteService.crearExpediente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(expedienteMapper.toDto(nuevo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar expediente por identificador UUID")
    public ResponseEntity<ExpedienteResponseDto> obtenerPorId(@PathVariable UUID id) {
        Expediente expediente = expedienteService.obtenerPorId(id);
        return ResponseEntity.ok(expedienteMapper.toDto(expediente));
    }

    @GetMapping("/tramite/{numeroTramite}")
    @Operation(summary = "Seguimiento ciudadano: Consultar estado por número de trámite (ej. EXP-2026-XXXXX)")
    public ResponseEntity<ExpedienteResponseDto> obtenerPorNumeroTramite(@PathVariable String numeroTramite) {
        Expediente expediente = expedienteService.obtenerPorNumeroTramite(numeroTramite);
        return ResponseEntity.ok(expedienteMapper.toDto(expediente));
    }

    @GetMapping
    @Operation(summary = "Listar expedientes con filtros opcionales de estado y alertas de vencimiento")
    public ResponseEntity<List<ExpedienteResponseDto>> listarExpedientes(
            @RequestParam(required = false) UUID solicitanteId,
            @RequestParam(required = false) EstadoExpediente estado,
            @RequestParam(required = false) Boolean conAlerta
    ) {
        List<Expediente> lista;
        if (solicitanteId != null) {
            lista = expedienteService.listarPorSolicitante(solicitanteId);
        } else if (estado != null || conAlerta != null) {
            lista = expedienteService.listarConFiltros(estado, conAlerta);
        } else {
            lista = expedienteService.listarTodos();
        }
        return ResponseEntity.ok(expedienteMapper.toDtoList(lista));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Auditoría: Consultar historial cronológico de estados y motivos")
    public ResponseEntity<List<HistorialEstadoDto>> obtenerHistorial(@PathVariable UUID id) {
        return ResponseEntity.ok(expedienteMapper.toHistorialDtoList(auditoriaService.obtenerHistorial(id)));
    }

    @GetMapping("/{id}/desglose-tasa")
    @Operation(summary = "US-06: Consultar desglose de conceptos tributarios TUPA según nivel de riesgo")
    public ResponseEntity<Map<String, BigDecimal>> obtenerDesgloseTasa(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        NivelRiesgo riesgo = exp.getNivelRiesgo() != null ? exp.getNivelRiesgo() : NivelRiesgo.BAJO;
        return ResponseEntity.ok(calculadoraDeTasa.calcularDesglose(riesgo));
    }

    @GetMapping("/{id}/documentos/declaracion-jurada")
    @Operation(summary = "US-05: Descargar en PDF el Anexo 1 oficial: Declaración Jurada para Licencia")
    public ResponseEntity<byte[]> descargarDeclaracionJurada(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarDeclaracionJurada(dto);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DeclaracionJurada-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/documentos/voucher-sat")
    @Operation(summary = "US-07: Descargar en PDF la Orden de Pago SAT con código de barras")
    public ResponseEntity<byte[]> descargarVoucherSat(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        VoucherDto voucher = expedienteService.generarVoucher(id);
        byte[] pdf = documentoPdfService.generarVoucherSatPdf(voucher);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=VoucherSAT-" + voucher.getVoucherId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/documentos/licencia")
    @Operation(summary = "US-09: Descargar en PDF el Certificado Oficial de Licencia de Funcionamiento con QR y Sello Digital")
    public ResponseEntity<byte[]> descargarLicencia(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        if (exp.getEstado() != EstadoExpediente.APROBADO) {
            throw new IllegalStateException("La licencia solo puede emitirse cuando el expediente ha sido APROBADO por la Gerencia de Licencias.");
        }
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarLicenciaPdf(dto);

        String codigo = exp.getLicenciaQrCode() != null ? exp.getLicenciaQrCode() : exp.getNumeroTramite();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Licencia-" + codigo + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "US-10: Obtener la imagen PNG del código QR único de la licencia emitida")
    public ResponseEntity<byte[]> obtenerImagenQrExpediente(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "300") int size
    ) {
        Expediente exp = expedienteService.obtenerPorId(id);
        String codigo = exp.getLicenciaQrCode() != null ? exp.getLicenciaQrCode() : exp.getNumeroTramite();
        byte[] qr = documentoPdfService.generarImagenQr(codigo, size, size);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qr);
    }

    @PostMapping("/{id}/clasificacion-riesgo")
    @Operation(summary = "Defensa Civil: Registrar clasificación de riesgo ITSE y calcular tasa")
    public ResponseEntity<Void> registrarClasificacionRiesgo(
            @PathVariable UUID id,
            @Valid @RequestBody ClasificacionRiesgoDto dto
    ) {
        expedienteService.registrarClasificacionRiesgo(id, dto.getNivelRiesgo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/voucher")
    @Operation(summary = "SAT: Generar voucher / orden de pago de la tasa administrativa")
    public ResponseEntity<VoucherDto> generarVoucher(@PathVariable UUID id) {
        VoucherDto voucher = expedienteService.generarVoucher(id);
        return ResponseEntity.ok(voucher);
    }

    @PostMapping("/{id}/pago")
    @Operation(summary = "SAT / Tesorería: Registrar constancia de pago de la tasa")
    public ResponseEntity<Void> registrarPago(
            @PathVariable UUID id,
            @Valid @RequestBody RegistroPagoDto dto
    ) {
        expedienteService.registrarPago(id, dto.getVoucherId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aprobar")
    @Operation(summary = "Gerencia de Licencias: Dictamen final favorable y emisión de licencia con QR")
    public ResponseEntity<Void> aprobar(@PathVariable UUID id) {
        expedienteService.aprobar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rechazar")
    @Operation(summary = "Gerencia de Licencias: Dictamen de rechazo u observaciones insubsanables")
    public ResponseEntity<Void> rechazar(
            @PathVariable UUID id,
            @Valid @RequestBody ResolucionExpedienteDto dto
    ) {
        expedienteService.rechazar(id, dto.getMotivo());
        return ResponseEntity.noContent().build();
    }
}
