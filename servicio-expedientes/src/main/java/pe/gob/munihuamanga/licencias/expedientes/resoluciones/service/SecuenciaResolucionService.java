package pe.gob.munihuamanga.licencias.expedientes.resoluciones.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.SecuenciaResolucion;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.SecuenciaResolucionRepository;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuenciaResolucionService {

    private final SecuenciaResolucionRepository secuenciaResolucionRepository;
    private final SecuenciaResolucionInicializadorHelper inicializadorHelper;

    @jakarta.annotation.PostConstruct
    public void inicializarSecuenciaPorDefecto() {
        int anioActual = LocalDate.now().getYear();
        inicializadorHelper.inicializarSecuenciaSiNoExiste(anioActual);
    }

    @Transactional
    public synchronized String generarSiguienteNumeroResolucion() {
        int anio = LocalDate.now().getYear();

        if (!secuenciaResolucionRepository.existsById(anio)) {
            inicializadorHelper.inicializarSecuenciaSiNoExiste(anio);
        }

        SecuenciaResolucion secuencia = secuenciaResolucionRepository.findByIdWithLock(anio)
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el bloqueo de secuencia de resolución para el año " + anio));

        int siguiente = secuencia.getUltimoNumero() + 1;
        secuencia.setUltimoNumero(siguiente);
        secuenciaResolucionRepository.saveAndFlush(secuencia);

        String correlativo = String.format("RES-%d-%05d", anio, siguiente);
        log.info("[SECUENCIA-RESOLUCION] Generado correlativo oficial: {}", correlativo);
        return correlativo;
    }
}
