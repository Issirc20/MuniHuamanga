package pe.gob.munihuamanga.licencias.common.enums;

import lombok.Getter;

/**
 * Estados del flujo de recepción, revisión y validación manual de pagos SAT (M05 - I06).
 * Regla técnica: La carga ciudadana de un comprobante registra el estado PENDIENTE_VALIDACION,
 * nunca un pago confirmado ni conciliación automática con el SAT.
 */
@Getter
public enum EstadoValidacionPago {

    PENDIENTE_VALIDACION("Comprobante registrado por el administrado, pendiente de revisión y validación manual"),
    EN_REVISION("Comprobante en proceso de revisión y cotejo por el cajero municipal"),
    VALIDADO_MANUALMENTE("Comprobante cotejado y validado manualmente por el cajero conforme a la tasa TUPA"),
    OBSERVADO("Comprobante observado por discrepancia de monto, ilegibilidad o inconsistencia de datos"),
    RECHAZADO("Comprobante rechazado por invalidez, duplicidad, prescripción o no imputable al trámite");

    private final String descripcion;

    EstadoValidacionPago(String descripcion) {
        this.descripcion = descripcion;
    }
}
