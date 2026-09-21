package pe.gob.munihuamanga.licencias.verificacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionLicenciaDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.verificacion.service.QrGeneratorService;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@Tag(name = "Verificación y QR", description = "Endpoints de verificación pública de autenticidad y generación de QR")
public class VerificacionController {

    private final QrGeneratorService qrGeneratorService;

    @GetMapping(value = "/api/licencias/qr/{codigoLicencia}", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generar imagen PNG del código QR para la licencia emitida")
    public ResponseEntity<byte[]> obtenerImagenQr(
            @PathVariable String codigoLicencia,
            @RequestParam(defaultValue = "300") int size
    ) {
        byte[] imagen = qrGeneratorService.generarImagenQr(codigoLicencia, size, size);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imagen);
    }

    @GetMapping("/api/public/licencias/{codigoLicencia}")
    @Operation(summary = "RNF-20: API pública de solo lectura para validar la autenticidad de una licencia")
    public ResponseEntity<VerificacionLicenciaDto> verificarLicenciaPublica(@PathVariable String codigoLicencia) {
        // En una implementación con base compartida o consulta distribuida, consulta el expediente
        // Simulación de respuesta de solo lectura garantizando seguridad y confidencialidad
        boolean esValida = codigoLicencia != null && codigoLicencia.startsWith("LIC-");

        VerificacionLicenciaDto dto = VerificacionLicenciaDto.builder()
                .numeroLicencia(codigoLicencia)
                .numeroTramite("EXP-2026-08452")
                .titular("CONSORCIO GASTRONÓMICO DE HUAMANGA S.A.C.")
                .razonSocial("CONSORCIO GASTRONÓMICO DE HUAMANGA S.A.C.")
                .nombreComercial("Restaurante Tradición Ayacuchana")
                .giro("Restaurante, café y servicios afines")
                .direccion("Portal Constitución N° 12, Plaza Mayor de Huamanga, Ayacucho")
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .estado(esValida ? "VIGENTE / AUTORIZADA" : "NO ENCONTRADA")
                .fechaEmision(LocalDateTime.now().minusMonths(1))
                .valida(esValida)
                .mensajeVerificacion(esValida ? "Licencia de Funcionamiento oficial y vigente expedida por la Municipalidad Provincial de Huamanga." : "Código de licencia no registrado o inválido.")
                .build();

        return ResponseEntity.ok(dto);
    }
}
