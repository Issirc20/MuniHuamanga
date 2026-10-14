package pe.gob.munihuamanga.licencias.expedientes.resoluciones.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.gob.munihuamanga.licencias.expedientes.licencias.service.SecuenciaLicenciaService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SecuenciaResolucionLicenciaMultiInstanciaTest {

    @Autowired
    private SecuenciaResolucionService secuenciaResolucionService;

    @Autowired
    private SecuenciaLicenciaService secuenciaLicenciaService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("I08 Concurrencia: Generación secuencial y atómica de correlativos de resolución RES-YYYY-XXXXX")
    void testGeneracionSecuenciaResolucion() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String num1 = tx.execute(status -> secuenciaResolucionService.generarSiguienteNumeroResolucion());
        String num2 = tx.execute(status -> secuenciaResolucionService.generarSiguienteNumeroResolucion());

        int anio = LocalDate.now().getYear();
        assertNotNull(num1);
        assertNotNull(num2);
        assertTrue(num1.startsWith("RES-" + anio + "-"));
        assertTrue(num2.startsWith("RES-" + anio + "-"));
        assertTrue(!num1.equals(num2), "Los números de resolución generados deben ser distintos");
    }

    @Test
    @DisplayName("I08 Concurrencia: Generación secuencial y atómica de correlativos de licencia LIC-YYYY-XXXXX")
    void testGeneracionSecuenciaLicencia() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String num1 = tx.execute(status -> secuenciaLicenciaService.generarSiguienteNumeroLicencia());
        String num2 = tx.execute(status -> secuenciaLicenciaService.generarSiguienteNumeroLicencia());

        int anio = LocalDate.now().getYear();
        assertNotNull(num1);
        assertNotNull(num2);
        assertTrue(num1.startsWith("LIC-" + anio + "-"));
        assertTrue(num2.startsWith("LIC-" + anio + "-"));
        assertTrue(!num1.equals(num2), "Los números de licencia generados deben ser distintos");
    }

    @Test
    @DisplayName("I08 Concurrencia: Hilos concurrentes generando resoluciones sin colisión")
    void testConcurrenciaMultihiloResolucion() throws InterruptedException {
        int hilos = 5;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(hilos);
        Set<String> numerosGenerados = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionTemplate tx = new TransactionTemplate(transactionManager);
                    String numero = tx.execute(status -> secuenciaResolucionService.generarSiguienteNumeroResolucion());
                    if (numero != null) {
                        numerosGenerados.add(numero);
                    }
                } catch (Exception e) {
                    // control de error
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completado = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completado);
        assertEquals(hilos, numerosGenerados.size(), "No debe haber duplicados en resoluciones");
    }

    @Test
    @DisplayName("I08 Concurrencia: Hilos concurrentes generando licencias sin colisión")
    void testConcurrenciaMultihiloLicencia() throws InterruptedException {
        int hilos = 5;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(hilos);
        Set<String> numerosGenerados = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionTemplate tx = new TransactionTemplate(transactionManager);
                    String numero = tx.execute(status -> secuenciaLicenciaService.generarSiguienteNumeroLicencia());
                    if (numero != null) {
                        numerosGenerados.add(numero);
                    }
                } catch (Exception e) {
                    // control de error
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completado = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completado);
        assertEquals(hilos, numerosGenerados.size(), "No debe haber duplicados en licencias");
    }
}
