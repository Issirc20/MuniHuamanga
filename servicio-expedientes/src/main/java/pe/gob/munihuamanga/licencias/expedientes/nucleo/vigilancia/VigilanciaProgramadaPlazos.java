package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.dto.AccionPropuestaDto;
import pe.gob.munihuamanga.licencias.common.enums.TipoAccionPropuesta;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ComandosExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;

/**
 * Ejecución diaria de la vigilancia de plazos.
 * Ejecuta automáticamente solo las acciones que no requieren decisión discrecional:
 * la conclusión del procedimiento y los avisos al administrado. Las demás quedan en la bandeja.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "munihuamanga.plazos.vigilancia-programada", havingValue = "true", matchIfMissing = true)
public class VigilanciaProgramadaPlazos {

    private final VigilanteDePlazos vigilante;
    private final ComandosExpediente comandosExpediente;
    private final ExpedienteRepository expedienteRepository;
    private final NotificacionEmailService notificacionEmailService;

    @Scheduled(cron = "${munihuamanga.plazos.cron-vigilancia:0 0 6 * * MON-FRI}", zone = "America/Lima")
    public void ejecutar() {
        int concluidos = 0;
        int avisos = 0;
        for (AccionPropuestaDto accion : vigilante.evaluarExpedientesEnTramite()) {
            try {
                if (accion.getAccion() == TipoAccionPropuesta.CONCLUIR_PROCEDIMIENTO) {
                    comandosExpediente.registrar(accion.getExpedienteId(), EventoExpediente.PROCEDIMIENTO_CONCLUIDO, accion.getDescripcion());
                    concluidos++;
                } else if (accion.getAccion() == TipoAccionPropuesta.AVISO_ADMINISTRADO && esPrimerDiaDeAviso(accion)) {
                    expedienteRepository.findById(accion.getExpedienteId())
                            .ifPresent(expediente -> notificacionEmailService.notificarAvisoPlazo(expediente, accion.getDescripcion()));
                    avisos++;
                }
            } catch (RuntimeException ex) {
                log.error("[VIGILANCIA-PLAZOS] No se pudo ejecutar la acción {} sobre el expediente {}: {}",
                        accion.getAccion(), accion.getNumeroTramite(), ex.getMessage());
            }
        }
        log.info("[VIGILANCIA-PLAZOS] Ejecución concluida: {} procedimientos concluidos, {} avisos enviados.", concluidos, avisos);
    }

    private static boolean esPrimerDiaDeAviso(AccionPropuestaDto accion) {
        return accion.getDiasTranscurridos() == (int) Math.ceil(accion.getDiasLimite() * 0.6);
    }
}
