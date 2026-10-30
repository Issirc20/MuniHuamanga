package pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio;

import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente.*;

/**
 * Catálogo cerrado de hechos que modifican el estado del expediente.
 * Cada evento declara los estados desde los que puede ocurrir y el estado al que conduce.
 * Incorporar un nuevo hecho del procedimiento consiste en agregar un valor a este catálogo,
 * sin modificar el agregado {@code Expediente}.
 */
public enum EventoExpediente {

    SOLICITUD_ADMITIDA(EnumSet.of(PRESENTADO), ITSE_EN_TRAMITE),
    SOLICITUD_OBSERVADA(EnumSet.of(PRESENTADO, EN_EVALUACION, SUBSANADO), OBSERVADO),
    ANEXOS_ITSE_CARGADOS(EnumSet.of(ITSE_EN_TRAMITE), ITSE_EN_REVISION),
    ITSE_OBSERVADO(EnumSet.of(ITSE_EN_REVISION), ITSE_EN_TRAMITE),
    ITSE_VALIDADO(EnumSet.of(ITSE_EN_REVISION), PENDIENTE_PAGO),
    PAGO_VALIDADO(EnumSet.of(PENDIENTE_PAGO), EN_EVALUACION),
    OBSERVACIONES_SUBSANADAS(EnumSet.of(OBSERVADO), SUBSANADO),
    /** Retorna el expediente al estado en que se encontraba cuando fue observado. */
    REVISION_REANUDADA(EnumSet.of(SUBSANADO), null),
    INFORME_APROBADO(EnumSet.of(EN_EVALUACION), APTO_PARA_RESOLUCION),
    RESOLUCION_FAVORABLE_FIRMADA(EnumSet.of(APTO_PARA_RESOLUCION), APROBADO),
    RESOLUCION_DENEGATORIA_FIRMADA(EnumSet.of(APTO_PARA_RESOLUCION), DENEGADO),
    LICENCIA_EMITIDA(EnumSet.of(APROBADO), EMITIDO),
    PROCEDIMIENTO_CONCLUIDO(EnumSet.of(EMITIDO), CONCLUIDO),
    ABANDONO_DECLARADO(EnumSet.of(ITSE_EN_TRAMITE, PENDIENTE_PAGO, OBSERVADO), ARCHIVADO),
    SOLICITUD_TENIDA_POR_NO_PRESENTADA(EnumSet.of(OBSERVADO), ARCHIVADO),
    DESISTIMIENTO_ACEPTADO(EnumSet.of(PRESENTADO, ITSE_EN_TRAMITE, ITSE_EN_REVISION, PENDIENTE_PAGO,
            EN_EVALUACION, OBSERVADO, SUBSANADO, APTO_PARA_RESOLUCION), ARCHIVADO);

    private final Set<EstadoExpediente> origenes;
    private final EstadoExpediente destino;

    EventoExpediente(Set<EstadoExpediente> origenes, EstadoExpediente destino) {
        this.origenes = Collections.unmodifiableSet(origenes);
        this.destino = destino;
    }

    public Set<EstadoExpediente> origenes() {
        return origenes;
    }

    /**
     * Estado de destino; es {@code null} cuando el destino depende del estado previo del expediente.
     */
    public EstadoExpediente destino() {
        return destino;
    }

    public boolean admiteOrigen(EstadoExpediente estado) {
        return origenes.contains(estado);
    }
}
