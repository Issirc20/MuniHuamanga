package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionLicenciaDto;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import pe.gob.munihuamanga.licencias.expedientes.service.ExpedienteService;

/**
 * US-11 / RNF-20: Controlador público de alta disponibilidad y solo lectura
 * para la verificación ciudadana y fiscalización de autenticidad de licencias de funcionamiento.
 */
@RestController
@RequestMapping("/api/public/licencias")
@RequiredArgsConstructor
@Tag(name = "Verificación Pública de Licencias", description = "Endpoints de solo lectura para consulta de autenticidad por código QR y ciudadanos (RNF-20)")
public class PublicLicenciasController {

    private final ExpedienteService expedienteService;
    private final DocumentoPdfService documentoPdfService;

    @GetMapping("/{codigoLicencia}")
    @Operation(summary = "RNF-20: API pública de solo lectura para verificar la autenticidad y vigencia de una licencia emitida")
    public ResponseEntity<VerificacionLicenciaDto> verificarLicencia(@PathVariable String codigoLicencia) {
        VerificacionLicenciaDto resultado = expedienteService.verificarLicencia(codigoLicencia);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping(value = "/{codigoLicencia}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "US-10: Obtener la imagen PNG del código QR para un código de licencia")
    public ResponseEntity<byte[]> obtenerQrPorCodigo(
            @PathVariable String codigoLicencia,
            @RequestParam(defaultValue = "300") int size
    ) {
        byte[] qr = documentoPdfService.generarImagenQr(codigoLicencia, size, size);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qr);
    }
}
