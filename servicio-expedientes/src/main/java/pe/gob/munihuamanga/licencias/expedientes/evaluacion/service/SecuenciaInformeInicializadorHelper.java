package pe.gob.munihuamanga.licencias.expedientes.evaluacion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.SecuenciaInforme;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.SecuenciaInformeRepository;

/**
 * Componente auxiliar para la inicialización atómica y aislada de secuencias anuales de informes técnicos.
 * Se ejecuta en transacción separada (REQUIRES_NEW) para proteger la transacción principal contra fallos de concurrencia.
 */
@Slf4j
@Component
public class SecuenciaInformeInicializadorHelper {

    private final SecuenciaInformeRepository secuenciaRepository;
    private final InformeAdministrativoRepository informeRepository;

    public SecuenciaInformeInicializadorHelper(SecuenciaInformeRepository secuenciaRepository,
                                              InformeAdministrativoRepository informeRepository) {
        this.secuenciaRepository = secuenciaRepository;
        this.informeRepository = informeRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asegurarSecuenciaParaAnio(int anio) {
        if (!secuenciaRepository.existsById(anio)) {
            try {
                long totalExistente = informeRepository.countByNumeroInformePrefix("INF-" + anio);
                int base = (int) totalExistente;
                secuenciaRepository.saveAndFlush(
                        SecuenciaInforme.builder()
                                .anio(anio)
                                .ultimoCorrelativo(base)
                                .build()
                );
                log.info("[CONCURRENCIA-INFORME] Secuencia asegurada atómicamente para año {} con base {}", anio, base);
            } catch (Exception ex) {
                log.debug("[CONCURRENCIA-INFORME] Fila de secuencia para año {} ya registrada concurrentemente", anio);
            }
        }
    }
}
