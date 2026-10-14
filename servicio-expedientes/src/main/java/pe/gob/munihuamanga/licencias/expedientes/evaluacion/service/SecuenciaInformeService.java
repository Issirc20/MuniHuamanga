package pe.gob.munihuamanga.licencias.expedientes.evaluacion.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.SecuenciaInforme;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.SecuenciaInformeRepository;

import java.time.LocalDate;

/**
 * Servicio de generación atómica y concurrente de correlativos para Informes Administrativos Técnicos (INF-YYYY-XXXXX).
 * Utiliza bloqueo pesimista en base de datos (SELECT FOR UPDATE) para eliminar el riesgo de duplicación por COUNT+1.
 */
@Slf4j
@Service
public class SecuenciaInformeService {

    private final SecuenciaInformeRepository secuenciaRepository;
    private final SecuenciaInformeInicializadorHelper inicializadorHelper;

    public SecuenciaInformeService(SecuenciaInformeRepository secuenciaRepository,
                                   SecuenciaInformeInicializadorHelper inicializadorHelper) {
        this.secuenciaRepository = secuenciaRepository;
        this.inicializadorHelper = inicializadorHelper;
    }

    @PostConstruct
    public void inicializarSecuenciaPorDefecto() {
        int anioActual = LocalDate.now().getYear();
        inicializadorHelper.asegurarSecuenciaParaAnio(anioActual);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public synchronized String generarSiguienteNumeroInforme() {
        int anioActual = LocalDate.now().getYear();

        // 1. Asegurar la existencia de la fila anual mediante transacción aislada (REQUIRES_NEW)
        if (!secuenciaRepository.existsById(anioActual)) {
            inicializadorHelper.asegurarSecuenciaParaAnio(anioActual);
        }

        // 2. Bloqueo pesimista FOR UPDATE garantizado sobre la fila del año
        SecuenciaInforme secuencia = secuenciaRepository.findByAnioWithLock(anioActual)
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el bloqueo de secuencia de informe para el año " + anioActual));

        int siguiente = secuencia.getUltimoCorrelativo() + 1;
        secuencia.setUltimoCorrelativo(siguiente);
        secuenciaRepository.saveAndFlush(secuencia);

        String numeroGenerado = String.format("INF-%d-%05d", anioActual, siguiente);
        log.info("[CONCURRENCIA-INFORME] Correlativo generado atómicamente: {}", numeroGenerado);
        return numeroGenerado;
    }
}
