package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ClasificacionRiesgoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialEstadoDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroPagoDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolucionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.SeguimientoCiudadanoDto;
import pe.gob.munihuamanga.licencias.common.dto.SubsanacionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.security.CustomUserDetails;
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
@Slf4j
@RestController
@RequestMapping("/api/expedientes")
@Tag(name = "Expedientes", description = "Endpoints para la gestión, seguimiento, formatos PDF y transiciones del trámite")
public class ExpedienteController {

    private final ExpedienteService expedienteService;
    private final ExpedienteMapper expedienteMapper;
    private final AuditoriaService auditoriaService;
    private final CalculadoraDeTasa calculadoraDeTasa;
    private final DocumentoPdfService documentoPdfService;
    private final pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository licenciaRepository;
    private final pe.gob.munihuamanga.licencias.expedientes.licencias.service.LicenciaService licenciaService;

    public ExpedienteController(ExpedienteService expedienteService,
                                ExpedienteMapper expedienteMapper,
                                AuditoriaService auditoriaService,
                                CalculadoraDeTasa calculadoraDeTasa,
                                DocumentoPdfService documentoPdfService,
                                pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository licenciaRepository,
                                pe.gob.munihuamanga.licencias.expedientes.licencias.service.LicenciaService licenciaService) {
        this.expedienteService = expedienteService;
        this.expedienteMapper = expedienteMapper;
        this.auditoriaService = auditoriaService;
        this.calculadoraDeTasa = calculadoraDeTasa;
        this.documentoPdfService = documentoPdfService;
        this.licenciaRepository = licenciaRepository;
        this.licenciaService = licenciaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    @Operation(summary = "Mesa de Partes Presencial: Registro directo por funcionario municipal (Justificación: Ingreso físico en ventanilla). Ciudadanos deben usar /api/borradores.")
    public ResponseEntity<ExpedienteResponseDto> crearExpediente(
            @Valid @RequestBody CrearExpedienteDto dto,
            Authentication authentication
    ) {
        boolean esAutenticado = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (!esAutenticado) {
            log.warn("[SEGURIDAD - ACCESO V2] Intento no autenticado de creación de expediente rechazado.");
            throw new AccessDeniedException(
                    "La creación formal de expedientes en V2 requiere autenticación previa y vinculación de titularidad de cuenta."
            );
        }

        // Cliente V2 autenticado: Vincular forzosamente titularidad con el usuario/cuenta autenticado
        if (authentication.getPrincipal() instanceof CustomUserDetails cud) {
            dto.setSolicitanteId(cud.getId());
        }

        Expediente nuevo = expedienteService.crearExpediente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(expedienteMapper.toDto(nuevo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar expediente por identificador UUID")
    public ResponseEntity<ExpedienteResponseDto> obtenerPorId(@PathVariable UUID id) {
        Expediente expediente = expedienteService.obtenerPorId(id);
        return ResponseEntity.ok(expedienteMapper.toDto(expediente));
    }

    @GetMapping("/tramite/{numeroTramite}")
    @Operation(summary = "Seguimiento ciudadano público: Consultar estado acotado por número de trámite (H07 / Ley N° 29733)")
    public ResponseEntity<SeguimientoCiudadanoDto> obtenerPorNumeroTramite(@PathVariable String numeroTramite) {
        Expediente expediente = expedienteService.obtenerPorNumeroTramite(numeroTramite);
        return ResponseEntity.ok(expedienteMapper.toSeguimientoDto(expediente));
    }

    @GetMapping("/detalle/{numeroTramite}")
    @PreAuthorize("@expedienteSecurity.puedeVerExpedientePorTramite(authentication, #numeroTramite)")
    @Operation(summary = "Consultar detalle completo de expediente por número de trámite para formatos oficiales")
    public ResponseEntity<ExpedienteResponseDto> obtenerDetallePorNumeroTramite(@PathVariable String numeroTramite) {
        Expediente expediente = expedienteService.obtenerPorNumeroTramite(numeroTramite);
        return ResponseEntity.ok(expedienteMapper.toDto(expediente));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN', 'CAJERO')")
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
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Auditoría: Consultar historial cronológico de estados y motivos")
    public ResponseEntity<List<HistorialEstadoDto>> obtenerHistorial(@PathVariable UUID id) {
        return ResponseEntity.ok(expedienteMapper.toHistorialDtoList(auditoriaService.obtenerHistorial(id)));
    }

    @GetMapping("/{id}/desglose-tasa")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "US-06: Consultar desglose de conceptos tributarios TUPA según nivel de riesgo")
    public ResponseEntity<Map<String, BigDecimal>> obtenerDesgloseTasa(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        NivelRiesgo riesgo = exp.getNivelRiesgo() != null ? exp.getNivelRiesgo() : NivelRiesgo.BAJO;
        return ResponseEntity.ok(calculadoraDeTasa.calcularDesglose(riesgo));
    }

    @GetMapping({ "/{id}/documentos/anexo1", "/{id}/documentos/anexo1-declaracion-jurada", "/{id}/documentos/declaracion-jurada" })
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar en PDF el Anexo 1 oficial de 2 páginas: Declaración Jurada para Licencia (Ley N° 28976 / D.S. N° 046-2017-PCM)")
    public ResponseEntity<byte[]> descargarAnexo1DeclaracionJurada(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarAnexo1DeclaracionJurada(dto);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo1-DeclaracionJurada-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/documentos/anexo3-matriz-riesgo-itse")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Fase 2: Descargar en PDF el Anexo 3 oficial (Matriz de Riesgo ITSE de 2 páginas según D.S. N° 002-2018-PCM)")
    public ResponseEntity<byte[]> descargarAnexo3MatrizRiesgoItse(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarAnexo3MatrizRiesgoItse(dto);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo3-MatrizRiesgoITSE-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/documentos/anexo4-condiciones-seguridad")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Fase 2: Descargar en PDF el Anexo 4 oficial (Declaración Jurada de Condiciones de Seguridad de 4 páginas)")
    public ResponseEntity<byte[]> descargarAnexo4CondicionesSeguridad(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarAnexo4CondicionesSeguridad(dto);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo4-CondicionesSeguridad-" + exp.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/documentos/voucher-sat")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "US-07: Descargar en PDF la Orden de Pago SAT con código de barras")
    public ResponseEntity<byte[]> descargarVoucherSat(@PathVariable UUID id) {
        VoucherDto voucher = expedienteService.generarVoucher(id);
        byte[] pdf = documentoPdfService.generarVoucherSatPdf(voucher);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=VoucherSAT-" + voucher.getVoucherId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/documentos/licencia")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "US-09: Descargar en PDF el Certificado Oficial de Licencia de Funcionamiento con QR y Sello Digital")
    public ResponseEntity<byte[]> descargarLicencia(@PathVariable UUID id) {
        Expediente exp = expedienteService.obtenerPorId(id);
        if (exp.getEstado() != EstadoExpediente.APROBADO) {
            throw new IllegalStateException("La licencia solo puede emitirse cuando el expediente ha sido APROBADO por la Gerencia de Licencias.");
        }
        if (!Boolean.TRUE.equals(exp.getFirmaDigitalValida()) || exp.getLicenciaQrCode() == null) {
            throw new IllegalStateException("El expediente " + exp.getNumeroTramite() + " se encuentra aprobado pero la emisión oficial del certificado definitivo está bloqueada: requiere firma digital institucional válida (Ley N° 27269 / DAS DA-06).");
        }

        // Prioridad: Si existe la licencia oficial emitida en I08, devolver el documento firmado inmutable
        java.util.Optional<pe.gob.munihuamanga.licencias.expedientes.licencias.model.LicenciaEmitida> optLic =
                licenciaRepository.findByExpedienteId(id);
        if (optLic.isPresent()) {
            byte[] pdf = licenciaService.descargarPdfLicencia(optLic.get().getId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Licencia-" + optLic.get().getNumeroLicencia() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        }

        // Fallback para expedientes pre-cargados / históricos aprobados
        ExpedienteResponseDto dto = expedienteMapper.toDto(exp);
        byte[] pdf = documentoPdfService.generarLicenciaPdf(dto);

        String codigo = exp.getLicenciaQrCode() != null ? exp.getLicenciaQrCode() : exp.getNumeroTramite();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Licencia-" + codigo + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = {"/{id}/qr", "/{id}/documentos/licencia-qr", "/{id}/documentos/qr"}, produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
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

    @org.springframework.web.bind.annotation.RequestMapping(
            value = {"/{id}/clasificar-riesgo", "/{id}/clasificacion-riesgo"},
            method = {org.springframework.web.bind.annotation.RequestMethod.PATCH, org.springframework.web.bind.annotation.RequestMethod.POST}
    )
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN')")
    @Operation(summary = "Defensa Civil: Registrar clasificación de riesgo ITSE y calcular tasa")
    public ResponseEntity<Void> registrarClasificacionRiesgo(
            @PathVariable UUID id,
            @Valid @RequestBody ClasificacionRiesgoDto dto
    ) {
        expedienteService.registrarClasificacionRiesgo(id, dto.getNivelRiesgo(), dto.getInformeItseNumero(), dto.getObservaciones());
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.RequestMapping(
            value = {"/{id}/voucher", "/{id}/generar-voucher"},
            method = {org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.GET}
    )
    @PreAuthorize("hasAnyRole('CAJERO', 'EVALUADOR', 'ADMIN')")
    @Operation(summary = "SAT: Generar voucher / orden de pago de la tasa administrativa")
    public ResponseEntity<VoucherDto> generarVoucher(@PathVariable UUID id) {
        VoucherDto voucher = expedienteService.generarVoucher(id);
        return ResponseEntity.ok(voucher);
    }

    @PostMapping(value = {"/{id}/pago", "/{id}/registrar-pago"})
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    @Operation(summary = "SAT / Tesorería: Registrar constancia de pago de la tasa")
    public ResponseEntity<Void> registrarPago(
            @PathVariable UUID id,
            @Valid @RequestBody RegistroPagoDto dto
    ) {
        expedienteService.registrarPago(id, dto.getVoucherId(), dto.getNumeroOperacionSat());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN')")
    @Operation(summary = "Gerencia de Licencias: Dictamen final favorable y pase a elaboración de resolución")
    public ResponseEntity<Void> aprobar(@PathVariable UUID id) {
        expedienteService.aprobar(id);
        return ResponseEntity.noContent().build();
    }

    @Deprecated(since = "2.0", forRemoval = true)
    @PostMapping("/{id}/emision-licencia")
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN')")
    @Operation(summary = "Ruta heredada V1 obsoleta: Emisión directa bloqueada por mandato de I08 (requiere resolución y firma digital)")
    public ResponseEntity<Map<String, Object>> emisionLicenciaHeredada(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of(
                "error", "EMISION_DIRECTA_DESHABILITADA",
                "codigo", HttpStatus.METHOD_NOT_ALLOWED.value(),
                "mensaje", "La emisión directa mediante la ruta heredada V1 está inhabilitada (Incremento I08.1 / Invariante 8). " +
                        "Toda licencia oficial requiere resolución administrativa formal debidamente firmada digitalmente (POST /api/resoluciones y POST /api/licencias/emitir)."
        ));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN')")
    @Operation(summary = "Gerencia de Licencias: Dictamen de rechazo / denegatoria de licencia")
    public ResponseEntity<Void> rechazar(
            @PathVariable UUID id,
            @Valid @RequestBody ResolucionExpedienteDto dto
    ) {
        expedienteService.rechazar(id, dto.getMotivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/observar")
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN')")
    @Operation(summary = "Gerencia de Licencias / Evaluador: Formular observaciones técnicas o documentales (Ley N° 27444 LPAG)")
    public ResponseEntity<Void> observar(
            @PathVariable UUID id,
            @Valid @RequestBody ResolucionExpedienteDto dto
    ) {
        expedienteService.observar(id, dto.getMotivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/subsanar")
    @PreAuthorize("@expedienteSecurity.puedeSubsanar(authentication, #id)")
    @Operation(summary = "Mesa de Partes / Administrado: Subsanación formal de observaciones (Art. 136 Ley N° 27444)")
    public ResponseEntity<Void> subsanar(
            @PathVariable UUID id,
            @Valid @RequestBody SubsanacionExpedienteDto dto
    ) {
        expedienteService.subsanar(id, dto.getDetalleSubsanacion(), dto.getUsuario());
        return ResponseEntity.noContent().build();
    }
}
