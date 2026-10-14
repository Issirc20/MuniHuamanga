package pe.gob.munihuamanga.licencias.expedientes.evaluacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.SecuenciaInformeRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.service.SecuenciaInformeService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecuenciaInformeMultiInstanciaTest {

    @Autowired
    private SecuenciaInformeService secuenciaInformeService;

    @Autowired
    private SecuenciaInformeRepository secuenciaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Generación de correlativo de informe asegura atómicamente la fila anual y genera formato INF-YYYY-XXXXX")
    void generacionCorrelativo_aseguraFilaAnualYFormatoCorrecto() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        String num1 = tx.execute(status -> secuenciaInformeService.generarSiguienteNumeroInforme());
        String num2 = tx.execute(status -> secuenciaInformeService.generarSiguienteNumeroInforme());

        int anio = LocalDate.now().getYear();
        assertThat(num1).isNotNull().startsWith(String.format("INF-%d-", anio));
        assertThat(num2).isNotNull().startsWith(String.format("INF-%d-", anio));
        assertThat(num1).isNotEqualTo(num2);
    }

    @Test
    @DisplayName("Concurrencia multihilo en informes no produce números repetidos bajo bloqueo pesimista")
    void concurrenciaMultihilo_correlativosInformesUnicos() throws InterruptedException {
        int hilos = 10;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(hilos);
        Set<String> numerosGenerados = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionTemplate tx = new TransactionTemplate(transactionManager);
                    String numero = tx.execute(status -> secuenciaInformeService.generarSiguienteNumeroInforme());
                    if (numero != null) {
                        numerosGenerados.add(numero);
                    }
                } catch (Exception e) {
                    // Control de error
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(numerosGenerados).hasSize(hilos);
    }
}
