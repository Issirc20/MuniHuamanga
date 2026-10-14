package pe.gob.munihuamanga.licencias.expedientes.borradores.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SecuenciaExpediente;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SecuenciaExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

/**
 * Componente auxiliar para la inicialización atómica y aislada de secuencias anuales.
 * Se ejecuta en transacción separada (REQUIRES_NEW) para proteger la transacción del trámite
 * contra fallos de concurrencia al momento de insertar el nuevo año en base de datos.
 */
@Slf4j
@Component
public class SecuenciaInicializadorHelper {

    private final SecuenciaExpedienteRepository secuenciaRepository;
    private final ExpedienteRepository expedienteRepository;

    public SecuenciaInicializadorHelper(SecuenciaExpedienteRepository secuenciaRepository,
                                       ExpedienteRepository expedienteRepository) {
        this.secuenciaRepository = secuenciaRepository;
        this.expedienteRepository = expedienteRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asegurarSecuenciaParaAnio(int anio) {
        if (!secuenciaRepository.existsById(anio)) {
            try {
                long totalExistente = expedienteRepository.count();
                int base = (int) Math.max(100, totalExistente + 10);
                secuenciaRepository.saveAndFlush(
                        SecuenciaExpediente.builder()
                                .anio(anio)
                                .ultimoCorrelativo(base)
                                .build()
                );
                log.info("[CONCURRENCIA-SECUENCIA] Secuencia asegurada atómicamente para año {} con base {}", anio, base);
            } catch (Exception ex) {
                log.debug("[CONCURRENCIA-SECUENCIA] Fila del año {} ya registrada por otra instancia/hilo concurrente", anio);
            }
        }
    }
}
