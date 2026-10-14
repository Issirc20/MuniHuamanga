package pe.gob.munihuamanga.licencias.expedientes.borradores;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SecuenciaExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.service.SecuenciaTramiteService;

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
class SecuenciaTramiteMultiInstanciaTest {

    @Autowired
    private SecuenciaTramiteService secuenciaService;

    @Autowired
    private SecuenciaExpedienteRepository secuenciaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Generación de correlativo sin depender de @PostConstruct asegura atómicamente la fila anual")
    void generacionCorrelativo_sinDependerDePostConstruct() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        String num1 = tx.execute(status -> secuenciaService.generarSiguienteNumeroExpediente());
        String num2 = tx.execute(status -> secuenciaService.generarSiguienteNumeroExpediente());

        assertThat(num1).isNotNull().startsWith("EXP-" + LocalDate.now().getYear() + "-");
        assertThat(num2).isNotNull().startsWith("EXP-" + LocalDate.now().getYear() + "-");
        assertThat(num1).isNotEqualTo(num2);
    }

    @Test
    @DisplayName("Concurrencia multihilo en generación de secuencias no produce correlativos repetidos")
    void concurrenciaMultihilo_correlativosUnicos() throws InterruptedException {
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
                    String numero = tx.execute(status -> secuenciaService.generarSiguienteNumeroExpediente());
                    if (numero != null) {
                        numerosGenerados.add(numero);
                    }
                } catch (Exception e) {
                    // Si ocurre colisión controlada
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
