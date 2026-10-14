package pe.gob.munihuamanga.licencias.expedientes.licencias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.SecuenciaLicencia;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.SecuenciaLicenciaRepository;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuenciaLicenciaService {

    private final SecuenciaLicenciaRepository secuenciaLicenciaRepository;
    private final SecuenciaLicenciaInicializadorHelper inicializadorHelper;

    @jakarta.annotation.PostConstruct
    public void inicializarSecuenciaPorDefecto() {
        int anioActual = LocalDate.now().getYear();
        inicializadorHelper.inicializarSecuenciaSiNoExiste(anioActual);
    }

    @Transactional
    public synchronized String generarSiguienteNumeroLicencia() {
        int anio = LocalDate.now().getYear();

        if (!secuenciaLicenciaRepository.existsById(anio)) {
            inicializadorHelper.inicializarSecuenciaSiNoExiste(anio);
        }

        SecuenciaLicencia secuencia = secuenciaLicenciaRepository.findByIdWithLock(anio)
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el bloqueo de secuencia de licencia para el año " + anio));

        int siguiente = secuencia.getUltimoNumero() + 1;
        secuencia.setUltimoNumero(siguiente);
        secuenciaLicenciaRepository.saveAndFlush(secuencia);

        String correlativo = String.format("LIC-%d-%05d", anio, siguiente);
        log.info("[SECUENCIA-LICENCIA] Generado correlativo de licencia oficial: {}", correlativo);
        return correlativo;
    }
}
