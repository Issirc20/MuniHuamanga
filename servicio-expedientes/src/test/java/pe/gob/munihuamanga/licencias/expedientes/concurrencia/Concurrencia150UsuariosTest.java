package pe.gob.munihuamanga.licencias.expedientes.concurrencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionLicenciaDto;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.ExpedienteService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-15 / RNF-01 / RNF-02 / H13:
 * Prueba de estres y alta concurrencia acoplada a Base de Datos Real (H2 en modo PostgreSQL / HikariCP).
 * Valida la capacidad objetivo de >= 150 usuarios concurrentes sin mocks en memoria,
 * verificando contencion de hilos, transacciones y latencia < 3.0s con 0% de errores.
 */
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("Pruebas de Carga y Concurrencia con Persistencia Real para >= 150 Usuarios (US-15, RNF-01, RNF-02, H13)")
class Concurrencia150UsuariosTest {

    private static final int USUARIOS_CONCURRENTES = 150;

    @Autowired
    private ExpedienteService expedienteService;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Test
    @DisplayName("H13 / RNF-01 / RNF-02: 150 peticiones simultaneas de verificacion de licencias contra BD real en < 3s con 0% error")
    void test150UsuariosConcurrentesVerificacionLicenciaRealDb() throws InterruptedException {
        String codigoLicencia = "LIC-2026-00000002";

        // Verificar que el registro exista realmente en la base de datos
        assertTrue(expedienteRepository.findByLicenciaQrCode(codigoLicencia).isPresent(),
                "El expediente de prueba con licencia LIC-2026-00000002 debe existir en la BD");

        ExecutorService executor = Executors.newFixedThreadPool(USUARIOS_CONCURRENTES);
        CountDownLatch barrier = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(USUARIOS_CONCURRENTES);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger fallos = new AtomicInteger(0);
        List<Long> latenciasMs = new CopyOnWriteArrayList<>();

        Instant inicioGlobal = Instant.now();

        for (int i = 0; i < USUARIOS_CONCURRENTES; i++) {
            executor.submit(() -> {
                try {
                    // Sincronizacion de barrera: asegurar disparo simultaneo real de los 150 hilos
                    barrier.await();

                    Instant inicioReq = Instant.now();
                    VerificacionLicenciaDto resultado = expedienteService.verificarLicencia(codigoLicencia);
                    long latencia = Duration.between(inicioReq, Instant.now()).toMillis();
                    latenciasMs.add(latencia);

                    if (resultado != null && resultado.isValida()) {
                        exitos.incrementAndGet();
                    } else {
                        fallos.incrementAndGet();
                    }
                } catch (Exception e) {
                    fallos.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Disparar las 150 peticiones concurrentes simultaneamente
        barrier.countDown();

        // Esperar a que terminen los 150 hilos (timeout de 15 segundos)
        boolean completado = endLatch.await(15, TimeUnit.SECONDS);
        Instant finGlobal = Instant.now();

        executor.shutdown();

        assertTrue(completado, "Las 150 peticiones concurrentes deben completarse antes del timeout");
        assertEquals(USUARIOS_CONCURRENTES, exitos.get(), "Las 150 peticiones deben haber sido procesadas exitosamente");
        assertEquals(0, fallos.get(), "No debe haber ningun fallo ni excepcion de conexion/timeout en BD");

        double latenciaMedia = latenciasMs.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long latenciaMax = latenciasMs.stream().mapToLong(Long::longValue).max().orElse(0L);
        long duracionTotalMs = Duration.between(inicioGlobal, finGlobal).toMillis();

        System.out.printf("[PRUEBA DE CARGA BD REAL] Peticiones: %d | Exitos: %d | Fallos: %d%n",
                USUARIOS_CONCURRENTES, exitos.get(), fallos.get());
        System.out.printf("[PRUEBA DE CARGA BD REAL] Duracion total: %d ms | Media: %.2f ms | Max: %d ms%n",
                duracionTotalMs, latenciaMedia, latenciaMax);

        // Cumplimiento del RNF-02: Latencia estrictamente menor a 3 segundos (3000 ms)
        assertTrue(latenciaMedia < 3000.0, "La latencia media en base de datos real debe ser menor a 3000 ms (RNF-02)");
    }

    @Test
    @DisplayName("H13 / RNF-01: 150 peticiones simultaneas de seguimiento ciudadano de expediente contra BD real")
    void test150UsuariosConcurrentesSeguimientoCiudadanoRealDb() throws InterruptedException {
        String numeroTramite = "EXP-2026-00001";

        assertTrue(expedienteRepository.findByNumeroTramite(numeroTramite).isPresent(),
                "El expediente EXP-2026-00001 debe existir en la BD");

        ExecutorService executor = Executors.newFixedThreadPool(USUARIOS_CONCURRENTES);
        CountDownLatch barrier = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(USUARIOS_CONCURRENTES);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger fallos = new AtomicInteger(0);
        List<Long> latenciasMs = new CopyOnWriteArrayList<>();

        Instant inicioGlobal = Instant.now();

        for (int i = 0; i < USUARIOS_CONCURRENTES; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();

                    Instant inicioReq = Instant.now();
                    Expediente resultado = expedienteService.obtenerPorNumeroTramite(numeroTramite);
                    long latencia = Duration.between(inicioReq, Instant.now()).toMillis();
                    latenciasMs.add(latencia);

                    if (resultado != null && numeroTramite.equals(resultado.getNumeroTramite())) {
                        exitos.incrementAndGet();
                    } else {
                        fallos.incrementAndGet();
                    }
                } catch (Exception e) {
                    fallos.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        barrier.countDown();
        boolean completado = endLatch.await(15, TimeUnit.SECONDS);
        Instant finGlobal = Instant.now();
        executor.shutdown();

        assertTrue(completado, "Las 150 peticiones concurrentes deben completarse antes del timeout");
        assertEquals(USUARIOS_CONCURRENTES, exitos.get(), "Las 150 peticiones de seguimiento deben ser exitosas");
        assertEquals(0, fallos.get(), "Cero fallos en consultas de seguimiento contra la BD");

        double latenciaMedia = latenciasMs.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long duracionTotalMs = Duration.between(inicioGlobal, finGlobal).toMillis();

        System.out.printf("[PRUEBA SEGUIMIENTO BD REAL] Peticiones: %d | Exitos: %d | Duracion: %d ms | Media: %.2f ms%n",
                USUARIOS_CONCURRENTES, exitos.get(), duracionTotalMs, latenciaMedia);

        assertTrue(latenciaMedia < 3000.0, "La latencia media debe ser menor a 3000 ms (RNF-02)");
    }
}
