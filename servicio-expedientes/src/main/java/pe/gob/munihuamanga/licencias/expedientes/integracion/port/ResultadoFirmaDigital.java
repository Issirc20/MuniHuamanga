package pe.gob.munihuamanga.licencias.expedientes.integracion.port;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Resultado estructurado del acto de firma digital institucional (DAS DA-06 / ERS RF-LIC-003).
 * Provee la evidencia criptográfica, hash SHA-256 e identidad del firmante oficial.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoFirmaDigital {

    private byte[] pdfFirmado;
    private String hashSha256;
    private String nombreFirmante;
    private String cargoFirmante;
    private String entidadCertificadora;
    private LocalDateTime fechaFirma;
    private boolean esValida;
    private String detalle;
}
