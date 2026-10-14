package pe.gob.munihuamanga.licencias.expedientes.integracion.port;

/**
 * Puerto hexagonal para la firma digital institucional (DAS DA-06 / ERS RF-LIC-003 / Incremento I08).
 * Desacoplado del proveedor criptográfico oficial (RENIEC, ONPE o Prestador de Servicios de Certificación Digital).
 * No almacena certificados en caliente ni simula firmas institucionales (Ley N° 27269).
 */
public interface FirmaDigitalPort {

    /**
     * Indica si el servicio institucional de firma digital está activo y debidamente configurado.
     */
    boolean estaDisponible();

    /**
     * Aplica la firma digital oficial al documento de licencia o expediente.
     *
     * @param pdfOriginal      Bytes del documento sin firmar.
     * @param numeroExpediente Número de trámite o expediente.
     * @return Bytes del documento firmado digitalmente.
     */
    byte[] firmarDocumento(byte[] pdfOriginal, String numeroExpediente);

    /**
     * Aplica la firma digital oficial a una resolución administrativa institucional.
     *
     * @param pdfOriginal       Bytes del documento original sin firmar.
     * @param numeroResolucion  Número oficial de la resolución.
     * @param motivo            Motivo o razón del acto resolutivo.
     * @param cargoFirmante     Cargo del funcionario titular firmante.
     * @return Resultado estructurado con bytes firmados, hash SHA-256 y evidencia.
     */
    ResultadoFirmaDigital firmarResolucion(byte[] pdfOriginal, String numeroResolucion, String motivo, String cargoFirmante);
}
