package pe.gob.munihuamanga.licencias.expedientes.documentos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expedientes/{id}/documentos")
@RequiredArgsConstructor
@Tag(name = "Documentos versionados", description = "Documentos del expediente con versionado inmutable y verificación SHA-256")
public class DocumentoVersionadoController {

    private final DocumentoVersionadoService documentoService;

    @PostMapping
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Adjuntar una nueva versión documental, según lo admita la etapa del expediente")
    public ResponseEntity<DocumentoVersionadoDto> adjuntarDocumento(@PathVariable UUID id,
                                                                    @Valid @RequestBody AdjuntarDocumentoVersionadoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.adjuntarDocumentoAExpediente(id, dto));
    }

    @GetMapping
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Listar los documentos del expediente (versiones actuales o historial completo)")
    public ResponseEntity<List<DocumentoVersionadoDto>> listarDocumentos(@PathVariable UUID id,
                                                                         @RequestParam(defaultValue = "true") boolean soloActuales) {
        return ResponseEntity.ok(documentoService.listarDocumentosExpediente(id, soloActuales));
    }

    @GetMapping("/{docId}")
    @PreAuthorize("@expedienteSecurity.puedeDescargarDocumento(authentication, #id)")
    @Operation(summary = "Descargar un documento con verificación de integridad SHA-256")
    public ResponseEntity<byte[]> descargarDocumento(@PathVariable UUID id, @PathVariable UUID docId) {
        DocumentoVersionadoDto dto = documentoService.descargarDocumento(docId);
        byte[] bytes = Base64.getDecoder().decode(dto.getContenidoBase64());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + dto.getNombreArchivo())
                .contentType(MediaType.parseMediaType(dto.getTipoContenido() != null ? dto.getTipoContenido() : "application/pdf"))
                .body(bytes);
    }

    @GetMapping("/historial/{tipoDocumento}")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar el historial de versiones de un tipo documental")
    public ResponseEntity<List<DocumentoVersionadoDto>> consultarHistorialVersiones(@PathVariable UUID id,
                                                                                    @PathVariable TipoDocumentoAdjunto tipoDocumento) {
        return ResponseEntity.ok(documentoService.listarHistorialVersiones(id, tipoDocumento));
    }
}
