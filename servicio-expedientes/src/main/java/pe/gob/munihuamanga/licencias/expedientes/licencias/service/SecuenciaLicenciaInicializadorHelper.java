package pe.gob.munihuamanga.licencias.expedientes.licencias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.SecuenciaLicencia;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.SecuenciaLicenciaRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecuenciaLicenciaInicializadorHelper {

    private final SecuenciaLicenciaRepository secuenciaLicenciaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inicializarSecuenciaSiNoExiste(int anio) {
        if (!secuenciaLicenciaRepository.existsById(anio)) {
            try {
                secuenciaLicenciaRepository.saveAndFlush(
                        SecuenciaLicencia.builder()
                                .anio(anio)
                                .ultimoNumero(0)
                                .build()
                );
                log.info("[SECUENCIA-LICENCIA] Secuencia de licencias inicializada para el año {}", anio);
            } catch (DataIntegrityViolationException ex) {
                log.debug("[SECUENCIA-LICENCIA] Secuencia de licencias del año {} ya fue inicializada concurrentemente", anio);
            }
        }
    }
}
