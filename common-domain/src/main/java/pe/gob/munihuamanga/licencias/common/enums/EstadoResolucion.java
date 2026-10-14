package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Estados del ciclo de vida de la resolución administrativa (M09 - Incremento I08).
 * Asegura la trazabilidad y la segregación estricta antes de la firma digital institucional.
 */
public enum EstadoResolucion {
    /**
     * En formulación de considerandos y referencias normativas por el funcionario.
     */
    BORRADOR,

    /**
     * Aprobada por la autoridad competente (Gerente), pendiente del acto de firma digital.
     */
    APROBADA_PENDIENTE_FIRMA,

    /**
     * Encolada o en proceso de comunicación con el proveedor institucional de firma digital PKI.
     */
    EN_PROCESO_FIRMA,

    /**
     * Debidamente firmada con certificado digital institucional y hash criptográfico verificado.
     */
    FIRMADA,

    /**
     * Fallo o rechazo en la firma digital (proveedor no disponible, token expirado, etc.).
     * Permite reintento controlado sin alterar la numeración oficial.
     */
    ERROR_FIRMA,

    /**
     * Anulada por causal administrativa o vicio de nulidad.
     */
    ANULADA
}
