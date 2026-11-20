package pe.gob.munihuamanga.licencias.expedientes.borradores.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SecuenciaExpediente;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SecuenciaExpedienteRepository;

import java.time.LocalDate;

/**
 * Servicio de generación de correlativos formales único y seguro ante accesos concurrentes.
 * Aplica bloqueo pesimista en base de datos para impedir duplicación de números EXP-YYYY-XXXXX.
 * Resiliente ante cambio de año y múltiples instancias concurrentes (no depende exclusivamente de @PostConstruct).
 */
@Slf4j
@Service
public class SecuenciaTramiteService {

    private final SecuenciaExpedienteRepository secuenciaRepository;
    private final SecuenciaInicializadorHelper inicializadorHelper;

    public SecuenciaTramiteService(SecuenciaExpedienteRepository secuenciaRepository,
                                  SecuenciaInicializadorHelper inicializadorHelper) {
        this.secuenciaRepository = secuenciaRepository;
        this.inicializadorHelper = inicializadorHelper;
    }

    @PostConstruct
    public void inicializarSecuenciaPorDefecto() {
        int anioActual = LocalDate.now().getYear();
        inicializadorHelper.asegurarSecuenciaParaAnio(anioActual);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public synchronized String generarSiguienteNumeroExpediente() {
        int anioActual = LocalDate.now().getYear();

        if (!secuenciaRepository.existsById(anioActual)) {
            inicializadorHelper.asegurarSecuenciaParaAnio(anioActual);
        }

        SecuenciaExpediente secuencia = secuenciaRepository.findByAnioWithLock(anioActual)
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el bloqueo de secuencia para el año " + anioActual));

        int siguiente = secuencia.getUltimoCorrelativo() + 1;
        secuencia.setUltimoCorrelativo(siguiente);
        secuenciaRepository.saveAndFlush(secuencia);

        String numeroGenerado = String.format("EXP-%d-%05d", anioActual, siguiente);
        log.info("[CONCURRENCIA-EXPEDIENTE] Correlativo generado atómicamente: {}", numeroGenerado);
        return numeroGenerado;
    }
}
