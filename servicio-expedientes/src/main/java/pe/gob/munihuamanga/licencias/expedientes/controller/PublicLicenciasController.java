package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionAmpliadaDto;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionLicenciaDto;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import pe.gob.munihuamanga.licencias.expedientes.service.ExpedienteService;

/**
 * US-11 / RNF-20: Controlador público heredado de verificación.
 * @deprecated En MuniHuamanga V2, la verificación oficial se realiza mediante {@code LicenciaController}
 * (/api/licencias/verificar/{codigoQr}) y {@code InspectorLicenciasController} (/api/inspector/licencias/verificar/{token})
 * según los lineamientos de privacidad de la Ley N° 29733 y el DAS (Incremento I09).
 */
@Deprecated(since = "2.0")
@RestController
@RequestMapping({"/api/public/licencias", "/api/public/verificar"})
@RequiredArgsConstructor
@Tag(name = "Verificación Pública de Licencias (Legada)", description = "Endpoints de compatibilidad con V1 para consulta pública")
public class PublicLicenciasController {

    private final ExpedienteService expedienteService;
    private final DocumentoPdfService documentoPdfService;

    @GetMapping("/{codigoLicencia}")
    @Operation(summary = "RNF-20 / RNF-SEG-007: API pública de solo lectura con datos mínimos para verificar autenticidad y vigencia")
    public ResponseEntity<VerificacionLicenciaDto> verificarLicencia(@PathVariable String codigoLicencia) {
        VerificacionLicenciaDto resultado = expedienteService.verificarLicencia(codigoLicencia);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{codigoLicencia}/ampliada")
    @PreAuthorize("hasAnyRole('INSPECTOR', 'EVALUADOR', 'ADMIN')")
    @Operation(summary = "RF-VER-007 / CA-10: Vista ampliada autorizada para inspectores y funcionarios con georreferenciación")
    public ResponseEntity<VerificacionAmpliadaDto> verificarLicenciaAmpliada(@PathVariable String codigoLicencia) {
        VerificacionAmpliadaDto resultado = expedienteService.verificarLicenciaAmpliada(codigoLicencia);
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
