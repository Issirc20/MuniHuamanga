package pe.gob.munihuamanga.licencias.expedientes.documentos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoVersionadoDto;
import pe.gob.munihuamanga.licencias.common.dto.DocumentoVersionadoDto;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.expedientes.documentos.service.DocumentoVersionadoService;
import pe.gob.munihuamanga.licencias.expedientes.security.CustomUserDetails;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de documentos versionados inmutables (Incremento I05).
 */
@RestController
@RequestMapping("/api/expedientes/{id}/documentos")
@Tag(name = "Documentos Versionados", description = "Gestión de documentos versionados inmutables y trazabilidad SHA-256")
public class DocumentoVersionadoController {

    private final DocumentoVersionadoService documentoService;

    public DocumentoVersionadoController(DocumentoVersionadoService documentoService) {
        this.documentoService = documentoService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adjuntar nueva versión documental a un expediente (solo en estado OBSERVADO para administrados)")
    public ResponseEntity<DocumentoVersionadoDto> adjuntarDocumento(
            @PathVariable UUID id,
            @Valid @RequestBody AdjuntarDocumentoVersionadoDto dto,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication.getName();
        String roles = extraerRoles(authentication);

        DocumentoVersionadoDto res = documentoService.adjuntarDocumentoAExpediente(id, dto, usuarioId, username, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Listar documentos de un expediente (actuales o histórico completo)")
    public ResponseEntity<List<DocumentoVersionadoDto>> listarDocumentos(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean soloActuales
    ) {
        return ResponseEntity.ok(documentoService.listarDocumentosExpediente(id, soloActuales));
    }

    @GetMapping("/{docId}")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar un documento versionado específico con verificación de integridad SHA-256")
    public ResponseEntity<byte[]> descargarDocumento(
            @PathVariable UUID id,
            @PathVariable UUID docId,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication.getName();
        String roles = extraerRoles(authentication);

        DocumentoVersionadoDto dto = documentoService.descargarDocumento(docId, usuarioId, username, roles);
        byte[] bytes = Base64.getDecoder().decode(dto.getContenidoBase64());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + dto.getNombreArchivo())
                .contentType(MediaType.parseMediaType(dto.getTipoContenido() != null ? dto.getTipoContenido() : "application/pdf"))
                .body(bytes);
    }

    @GetMapping("/historial/{tipoDocumento}")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar el historial completo de versiones de un tipo documental en el expediente")
    public ResponseEntity<List<DocumentoVersionadoDto>> consultarHistorialVersiones(
            @PathVariable UUID id,
            @PathVariable TipoDocumentoAdjunto tipoDocumento
    ) {
        return ResponseEntity.ok(documentoService.listarHistorialVersiones(id, tipoDocumento));
    }

    private UUID extraerUsuarioId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }

    private String extraerRoles(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return "";
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
    }
}
