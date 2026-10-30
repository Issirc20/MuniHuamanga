package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialEstadoDto;
import pe.gob.munihuamanga.licencias.common.dto.SeguimientoCiudadanoDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.LicenciaEmitida;
import pe.gob.munihuamanga.licencias.expedientes.licencias.service.LicenciaService;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.service.CalculadoraDeTasa;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import pe.gob.munihuamanga.licencias.expedientes.service.ExpedienteService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consulta de expedientes y descarga de sus documentos.
 * Las actuaciones que modifican el estado se exponen en los controladores de cada etapa.
 */
@RestController
@RequestMapping("/api/expedientes")
@RequiredArgsConstructor
@Tag(name = "Expedientes", description = "Consulta de expedientes, trazabilidad y documentos")
public class ExpedienteController {

    private static final String ROLES_CONSULTA =
            "hasAnyRole('MESA_DE_PARTES', 'EVALUADOR', 'SUPERVISOR_LICENCIAS', 'AUTORIDAD_RESOLUTIVA', 'CAJERO', 'AUDITOR')";

    private final ExpedienteService expedienteService;
    private final ExpedienteMapper expedienteMapper;
    private final AuditoriaService auditoriaService;
    private final CalculadoraDeTasa calculadoraDeTasa;
    private final DocumentoPdfService documentoPdfService;
    private final LicenciaService licenciaService;
    private final ProveedorActor proveedorActor;

    @GetMapping("/{id}")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar expediente por identificador")
    public ResponseEntity<ExpedienteResponseDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(expedienteMapper.toDto(expedienteService.obtenerPorId(id)));
    }

    @GetMapping("/tramite/{numeroTramite}")
    @Operation(summary = "Seguimiento público con datos mínimos: requiere los cuatro últimos dígitos del documento del titular")
    public ResponseEntity<SeguimientoCiudadanoDto> seguimientoPublico(@PathVariable String numeroTramite,
                                                                     @RequestParam("documento") String digitosDocumento) {
        Expediente expediente = expedienteService.obtenerParaSeguimientoPublico(numeroTramite, digitosDocumento);
        return ResponseEntity.ok(expedienteMapper.toSeguimientoDto(expediente));
    }

    @GetMapping("/detalle/{numeroTramite}")
    @PreAuthorize("@expedienteSecurity.puedeVerExpedientePorTramite(authentication, #numeroTramite)")
    @Operation(summary = "Consultar el detalle completo de un expediente por número de trámite")
    public ResponseEntity<ExpedienteResponseDto> obtenerDetallePorNumeroTramite(@PathVariable String numeroTramite) {
        return ResponseEntity.ok(expedienteMapper.toDto(expedienteService.obtenerPorNumeroTramite(numeroTramite)));
    }

    @GetMapping
    @PreAuthorize(ROLES_CONSULTA)
    @Operation(summary = "Listar expedientes con filtros de estado y alerta de vencimiento del plazo legal")
    public ResponseEntity<List<ExpedienteResponseDto>> listarExpedientes(
            @RequestParam(required = false) EstadoExpediente estado,
            @RequestParam(required = false) Boolean conAlerta) {
        return ResponseEntity.ok(expedienteMapper.toDtoList(expedienteService.listarConFiltros(estado, conAlerta)));
    }

    @GetMapping("/mis-expedientes")
    @PreAuthorize("hasRole('CIUDADANO')")
    @Operation(summary = "Listar los expedientes de las cuentas que representa la persona autenticada")
    public ResponseEntity<List<ExpedienteResponseDto>> misExpedientes() {
        UUID usuarioId = proveedorActor.actual().usuarioId();
        return ResponseEntity.ok(expedienteMapper.toDtoList(expedienteService.listarDeCuentasRepresentadas(usuarioId)));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar la trazabilidad cronológica de estados del expediente")
    public ResponseEntity<List<HistorialEstadoDto>> obtenerHistorial(@PathVariable UUID id) {
        return ResponseEntity.ok(expedienteMapper.toHistorialDtoList(auditoriaService.obtenerHistorial(id)));
    }

    @GetMapping("/{id}/desglose-tasa")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar el desglose de la tasa según el nivel de riesgo oficial")
    public ResponseEntity<Map<String, BigDecimal>> obtenerDesgloseTasa(@PathVariable UUID id) {
        Expediente expediente = expedienteService.obtenerPorId(id);
        if (expediente.getNivelRiesgo() == null) {
            throw new IllegalStateException("El nivel de riesgo oficial aún no ha sido determinado para el expediente "
                    + expediente.getNumeroTramite() + ".");
        }
        return ResponseEntity.ok(calculadoraDeTasa.calcularDesglose(expediente.getNivelRiesgo()));
    }

    @GetMapping({"/{id}/documentos/anexo1", "/{id}/documentos/declaracion-jurada"})
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar el Anexo 1: declaración jurada para licencia de funcionamiento")
    public ResponseEntity<byte[]> descargarAnexo1(@PathVariable UUID id) {
        Expediente expediente = expedienteService.obtenerPorId(id);
        byte[] pdf = documentoPdfService.generarAnexo1DeclaracionJurada(expedienteMapper.toDto(expediente));
        return pdf("Anexo1-DeclaracionJurada-" + expediente.getNumeroTramite() + ".pdf", pdf);
    }

    @GetMapping("/{id}/documentos/anexo3-matriz-riesgo-itse")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar el formato prellenado del Anexo 3 ITSE para su firma ante Defensa Civil")
    public ResponseEntity<byte[]> descargarAnexo3(@PathVariable UUID id) {
        Expediente expediente = expedienteService.obtenerPorId(id);
        byte[] pdf = documentoPdfService.generarAnexo3MatrizRiesgoItse(expedienteMapper.toDto(expediente));
        return pdf("Anexo3-MatrizRiesgoITSE-" + expediente.getNumeroTramite() + ".pdf", pdf);
    }

    @GetMapping("/{id}/documentos/anexo4-condiciones-seguridad")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar el formato prellenado del Anexo 4 ITSE para su firma ante Defensa Civil")
    public ResponseEntity<byte[]> descargarAnexo4(@PathVariable UUID id) {
        Expediente expediente = expedienteService.obtenerPorId(id);
        byte[] pdf = documentoPdfService.generarAnexo4CondicionesSeguridad(expedienteMapper.toDto(expediente));
        return pdf("Anexo4-CondicionesSeguridad-" + expediente.getNumeroTramite() + ".pdf", pdf);
    }

    @GetMapping("/{id}/documentos/orden-pago")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar la orden de pago de la tasa liquidada")
    public ResponseEntity<byte[]> descargarOrdenPago(@PathVariable UUID id) {
        VoucherDto orden = expedienteService.generarOrdenPago(id);
        return pdf("OrdenPago-" + orden.getNumeroTramite() + ".pdf", documentoPdfService.generarVoucherSatPdf(orden));
    }

    @GetMapping("/{id}/orden-pago")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar los datos de la orden de pago de la tasa liquidada")
    public ResponseEntity<VoucherDto> consultarOrdenPago(@PathVariable UUID id) {
        return ResponseEntity.ok(expedienteService.generarOrdenPago(id));
    }

    @GetMapping("/{id}/documentos/licencia")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar la licencia de funcionamiento emitida y firmada")
    public ResponseEntity<byte[]> descargarLicencia(@PathVariable UUID id) {
        LicenciaEmitida licencia = licenciaService.obtenerEntidadPorExpediente(id);
        byte[] pdf = licenciaService.descargarPdfLicencia(licencia.getId());
        return pdf("Licencia-" + licencia.getNumeroLicencia() + ".pdf", pdf);
    }

    @GetMapping(value = {"/{id}/qr", "/{id}/documentos/licencia-qr"}, produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Obtener la imagen del código de verificación de la licencia emitida")
    public ResponseEntity<byte[]> obtenerImagenQr(@PathVariable UUID id, @RequestParam(defaultValue = "300") int size) {
        int lado = Math.max(120, Math.min(size, 600));
        LicenciaEmitida licencia = licenciaService.obtenerEntidadPorExpediente(id);
        if (licencia.getCodigoVerificacionQr() == null) {
            throw new RecursoNoEncontradoException("La licencia del expediente no cuenta con código de verificación.");
        }
        byte[] qr = documentoPdfService.generarImagenQr(licencia.getCodigoVerificacionQr(), lado, lado);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qr);
    }

    private static ResponseEntity<byte[]> pdf(String nombreArchivo, byte[] contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}
