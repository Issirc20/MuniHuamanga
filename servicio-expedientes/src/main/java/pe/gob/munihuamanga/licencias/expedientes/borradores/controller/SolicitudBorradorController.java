package pe.gob.munihuamanga.licencias.expedientes.borradores.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.DocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.PresentarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitudBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidacionRequisitosDto;
import pe.gob.munihuamanga.licencias.expedientes.borradores.service.PresentacionFormalService;
import pe.gob.munihuamanga.licencias.expedientes.borradores.service.SolicitudBorradorService;
import pe.gob.munihuamanga.licencias.expedientes.security.CustomUserDetails;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/borradores")
@RequiredArgsConstructor
@Tag(name = "Borradores de Licencia", description = "Gestión de borradores preliminares, guardado parcial y presentación formal (M03 y M06)")
public class SolicitudBorradorController {

    private final SolicitudBorradorService borradorService;
    private final PresentacionFormalService presentacionFormalService;
    private final DocumentoPdfService documentoPdfService;

    @PostMapping
    @PreAuthorize("@borradorSecurity.puedeCrearBorradorEnCuenta(authentication, #dto.cuentaId)")
    @Operation(summary = "Crear nuevo borrador de solicitud (No genera número EXP-*)")
    public ResponseEntity<SolicitudBorradorDto> crearBorrador(
            @Valid @RequestBody CrearBorradorDto dto,
            Authentication authentication
    ) {
        UUID usuarioId = obtenerUsuarioId(authentication);
        SolicitudBorradorDto creado = borradorService.crearBorrador(dto, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@borradorSecurity.puedeEditarBorrador(authentication, #id)")
    @Operation(summary = "Guardar avances parciales del borrador")
    public ResponseEntity<SolicitudBorradorDto> guardarAvance(
            @PathVariable UUID id,
            @RequestBody ActualizarBorradorDto dto,
            Authentication authentication
    ) {
        UUID usuarioId = obtenerUsuarioId(authentication);
        SolicitudBorradorDto actualizado = borradorService.guardarAvance(id, dto, usuarioId);
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@borradorSecurity.puedeVerBorrador(authentication, #id)")
    @Operation(summary = "Recuperar borrador por identificador UUID")
    public ResponseEntity<SolicitudBorradorDto> obtenerBorrador(@PathVariable UUID id) {
        SolicitudBorradorDto borrador = borradorService.obtenerBorrador(id);
        return ResponseEntity.ok(borrador);
    }

    @GetMapping("/cuenta/{cuentaId}")
    @PreAuthorize("@cuentaSecurity.puedeVerCuenta(authentication, #cuentaId)")
    @Operation(summary = "Listar borradores asociados a una cuenta titular")
    public ResponseEntity<List<SolicitudBorradorDto>> listarPorCuenta(@PathVariable UUID cuentaId) {
        List<SolicitudBorradorDto> lista = borradorService.listarPorCuenta(cuentaId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}/requisitos-pendientes")
    @PreAuthorize("@borradorSecurity.puedeVerBorrador(authentication, #id)")
    @Operation(summary = "Validar requisitos pendientes para la presentación formal")
    public ResponseEntity<ValidacionRequisitosDto> validarRequisitosPendientes(@PathVariable UUID id) {
        ValidacionRequisitosDto validacion = borradorService.validarRequisitos(id);
        return ResponseEntity.ok(validacion);
    }

    @PostMapping("/{id}/documentos")
    @PreAuthorize("@borradorSecurity.puedeEditarBorrador(authentication, #id)")
    @Operation(summary = "Adjuntar documento probatorio al borrador con cálculo SHA-256")
    public ResponseEntity<DocumentoBorradorDto> adjuntarDocumento(
            @PathVariable UUID id,
            @Valid @RequestBody AdjuntarDocumentoBorradorDto dto,
            Authentication authentication
    ) {
        UUID usuarioId = obtenerUsuarioId(authentication);
        DocumentoBorradorDto doc = borradorService.adjuntarDocumento(id, dto, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @GetMapping("/{id}/documentos")
    @PreAuthorize("@borradorSecurity.puedeVerBorrador(authentication, #id)")
    @Operation(summary = "Listar documentos adjuntos al borrador")
    public ResponseEntity<List<DocumentoBorradorDto>> listarDocumentos(@PathVariable UUID id) {
        List<DocumentoBorradorDto> documentos = borradorService.listarDocumentos(id);
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/{id}/documentos/{docId}")
    @PreAuthorize("@borradorSecurity.puedeVerBorrador(authentication, #id)")
    @Operation(summary = "Consultar documento adjunto y verificar integridad y trazabilidad de acceso")
    public ResponseEntity<DocumentoBorradorDto> obtenerDocumento(
            @PathVariable UUID id,
            @PathVariable UUID docId,
            Authentication authentication
    ) {
        UUID usuarioId = obtenerUsuarioId(authentication);
        String rol = authentication.getAuthorities().stream().findFirst().map(Object::toString).orElse("");
        DocumentoBorradorDto doc = borradorService.descargarDocumento(id, docId, usuarioId, rol);
        return ResponseEntity.ok(doc);
    }

    @DeleteMapping("/{id}/documentos/{docId}")
    @PreAuthorize("@borradorSecurity.puedeEditarBorrador(authentication, #id)")
    @Operation(summary = "Eliminar documento del borrador")
    public ResponseEntity<Void> eliminarDocumento(
            @PathVariable UUID id,
            @PathVariable UUID docId,
            Authentication authentication
    ) {
        UUID usuarioId = obtenerUsuarioId(authentication);
        borradorService.eliminarDocumento(id, docId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@borradorSecurity.puedeEditarBorrador(authentication, #id)")
    @Operation(summary = "Descartar borrador conservando trazabilidad de auditoría")
    public ResponseEntity<Void> descartarBorrador(
            @PathVariable UUID id,
            @RequestParam(required = false) String motivo,
            Authentication authentication
    ) {
        UUID usuarioId = obtenerUsuarioId(authentication);
        borradorService.descartarBorrador(id, motivo, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/presentar")
    @PreAuthorize("@borradorSecurity.puedePresentarBorrador(authentication, #id)")
    @Operation(summary = "Presentación formal transaccional de la solicitud (Genera número EXP-* único e inmutable)")
    public ResponseEntity<ExpedienteResponseDto> presentarBorrador(
            @PathVariable UUID id,
            @Valid @RequestBody PresentarBorradorDto dto,
            Authentication authentication
    ) {
        UUID usuarioId = obtenerUsuarioId(authentication);
        String username = authentication.getName();
        ExpedienteResponseDto expediente = presentacionFormalService.presentarBorrador(id, dto, usuarioId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(expediente);
    }

    @GetMapping("/{id}/formularios/anexo-1/pdf")
    @PreAuthorize("@borradorSecurity.puedeVerBorrador(authentication, #id)")
    @Operation(summary = "Generar y descargar PDF oficial del Anexo 1 Declaración Jurada reutilizando datos del borrador")
    public ResponseEntity<byte[]> descargarAnexo1Borrador(@PathVariable UUID id) {
        ExpedienteResponseDto dto = borradorService.convertirBorradorAExpedienteResponseDto(id);
        byte[] pdf = documentoPdfService.generarAnexo1DeclaracionJurada(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo1-DeclaracionJurada-" + dto.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/formularios/anexo-3/pdf")
    @PreAuthorize("@borradorSecurity.puedeVerBorrador(authentication, #id)")
    @Operation(summary = "Generar y descargar PDF oficial del Anexo 3 Matriz de Riesgo ITSE reutilizando datos del borrador")
    public ResponseEntity<byte[]> descargarAnexo3Borrador(@PathVariable UUID id) {
        ExpedienteResponseDto dto = borradorService.convertirBorradorAExpedienteResponseDto(id);
        byte[] pdf = documentoPdfService.generarAnexo3MatrizRiesgoItse(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo3-MatrizRiesgoITSE-" + dto.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/formularios/anexo-4/pdf")
    @PreAuthorize("@borradorSecurity.puedeVerBorrador(authentication, #id)")
    @Operation(summary = "Generar y descargar PDF oficial del Anexo 4 Condiciones de Seguridad ITSE reutilizando datos del borrador")
    public ResponseEntity<byte[]> descargarAnexo4Borrador(@PathVariable UUID id) {
        ExpedienteResponseDto dto = borradorService.convertirBorradorAExpedienteResponseDto(id);
        byte[] pdf = documentoPdfService.generarAnexo4CondicionesSeguridad(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Anexo4-CondicionesSeguridad-" + dto.getNumeroTramite() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private UUID obtenerUsuarioId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }
}
