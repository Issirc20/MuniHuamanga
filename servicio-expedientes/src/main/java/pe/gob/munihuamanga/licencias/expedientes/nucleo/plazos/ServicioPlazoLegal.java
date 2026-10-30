package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.PlazoProcedimientoDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.model.HistorialEstado;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.HistorialRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Reconstruye el plazo legal de evaluación a partir del historial de estados:
 * corre desde que el expediente está completo (ingreso a EN_EVALUACION), se suspende
 * mientras está OBSERVADO y concluye con la resolución o el archivo.
 */
@Service
@RequiredArgsConstructor
public class ServicioPlazoLegal {

    static final Set<EstadoExpediente> ESTADOS_EN_CURSO = EnumSet.of(
            EstadoExpediente.EN_EVALUACION, EstadoExpediente.SUBSANADO, EstadoExpediente.APTO_PARA_RESOLUCION);

    private static final Set<EstadoExpediente> ESTADOS_DE_CIERRE = EnumSet.of(
            EstadoExpediente.APROBADO, EstadoExpediente.DENEGADO, EstadoExpediente.EMITIDO,
            EstadoExpediente.CONCLUIDO, EstadoExpediente.ARCHIVADO);

    private final HistorialRepository historialRepository;
    private final ExpedienteRepository expedienteRepository;
    private final CalculadoraPlazos calculadora;
    private final ParametrosPlazoService parametros;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public PlazoProcedimientoDto calcular(UUID expedienteId) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", expedienteId));
        return calcular(expediente);
    }

    @Transactional(readOnly = true)
    public PlazoProcedimientoDto calcular(Expediente expediente) {
        List<HistorialEstado> historial = historialRepository.findByExpedienteIdOrderByFechaAsc(expediente.getId());
        int diasPlazo = parametros.dias(CodigoPlazo.PLAZO_LEGAL_EVALUACION);
        LocalDate hoy = LocalDate.now(reloj);

        LocalDate inicio = null;
        LocalDate inicioTramoActual = null;
        int consumidos = 0;
        boolean concluido = false;

        for (HistorialEstado registro : historial) {
            if (registro.getEstadoAnterior() == registro.getEstadoNuevo() || registro.getEstadoNuevo() == null) {
                continue;
            }
            LocalDate fecha = registro.getFecha().toLocalDate();
            boolean entraEnCurso = ESTADOS_EN_CURSO.contains(registro.getEstadoNuevo());

            if (inicioTramoActual != null && !entraEnCurso) {
                consumidos += calculadora.contarDiasHabiles(inicioTramoActual, fecha);
                inicioTramoActual = null;
            }
            if (entraEnCurso && inicioTramoActual == null) {
                if (inicio == null) {
                    inicio = fecha;
                }
                inicioTramoActual = fecha;
            }
            if (ESTADOS_DE_CIERRE.contains(registro.getEstadoNuevo()) && inicio != null) {
                concluido = true;
            }
        }

        boolean enCurso = inicioTramoActual != null;
        if (enCurso) {
            consumidos += calculadora.contarDiasHabiles(inicioTramoActual, hoy);
        }

        int restantes = Math.max(0, diasPlazo - consumidos);
        LocalDate vencimiento = (inicio == null || concluido) ? null : calculadora.sumarDiasHabiles(hoy, restantes);

        return PlazoProcedimientoDto.builder()
                .expedienteId(expediente.getId())
                .numeroTramite(expediente.getNumeroTramite())
                .estado(expediente.getEstado())
                .iniciado(inicio != null)
                .fechaInicio(inicio)
                .diasPlazo(diasPlazo)
                .diasConsumidos(consumidos)
                .diasRestantes(restantes)
                .fechaVencimientoEstimada(vencimiento)
                .suspendido(inicio != null && !enCurso && !concluido)
                .concluido(concluido)
                .vencido(inicio != null && !concluido && consumidos >= diasPlazo)
                .build();
    }

    /**
     * Actualiza la fecha límite proyectada del expediente. Solo existe mientras el plazo corre.
     */
    @Transactional
    public void actualizarFechaLimite(Expediente expediente) {
        PlazoProcedimientoDto plazo = calcular(expediente);
        boolean corre = plazo.isIniciado() && !plazo.isSuspendido() && !plazo.isConcluido();
        expediente.setFechaLimite(corre && plazo.getFechaVencimientoEstimada() != null
                ? plazo.getFechaVencimientoEstimada().atTime(LocalTime.of(23, 59))
                : null);
        expedienteRepository.save(expediente);
    }

    /** Fecha del último ingreso del expediente a su estado actual. */
    @Transactional(readOnly = true)
    public Optional<LocalDate> fechaIngresoEstadoActual(Expediente expediente) {
        return historialRepository
                .findFirstByExpedienteIdAndEstadoNuevoOrderByFechaDesc(expediente.getId(), expediente.getEstado())
                .map(registro -> registro.getFecha().toLocalDate());
    }

    public int diasHabilesDesde(LocalDate fecha) {
        return calculadora.contarDiasHabiles(fecha, LocalDate.now(reloj));
    }
}
