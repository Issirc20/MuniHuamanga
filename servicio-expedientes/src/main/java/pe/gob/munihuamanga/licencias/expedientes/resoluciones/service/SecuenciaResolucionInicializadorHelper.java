package pe.gob.munihuamanga.licencias.expedientes.resoluciones.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.SecuenciaResolucion;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.SecuenciaResolucionRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecuenciaResolucionInicializadorHelper {

    private final SecuenciaResolucionRepository secuenciaResolucionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inicializarSecuenciaSiNoExiste(int anio) {
        if (!secuenciaResolucionRepository.existsById(anio)) {
            try {
                secuenciaResolucionRepository.saveAndFlush(
                        SecuenciaResolucion.builder()
                                .anio(anio)
                                .ultimoNumero(0)
                                .build()
                );
                log.info("[SECUENCIA-RESOLUCION] Secuencia inicializada para el año {}", anio);
            } catch (DataIntegrityViolationException ex) {
                log.debug("[SECUENCIA-RESOLUCION] Secuencia del año {} ya fue inicializada concurrentemente", anio);
            }
        }
    }
}
