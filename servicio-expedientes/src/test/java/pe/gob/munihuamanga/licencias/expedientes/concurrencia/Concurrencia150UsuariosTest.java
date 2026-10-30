package pe.gob.munihuamanga.licencias.expedientes.concurrencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ServicioPlazoLegal;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.ExpedienteService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de concurrencia con persistencia real (H2 en modo PostgreSQL y pool HikariCP):
 * 150 consultas simultáneas deben resolverse sin errores y con latencia media inferior a 3 segundos.
 */
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("Concurrencia con persistencia real para 150 usuarios simultáneos")
class Concurrencia150UsuariosTest {

    private static final int USUARIOS_CONCURRENTES = 150;
    private static final String DIGITOS_DOCUMENTO = "7891";

    @Autowired
    private ExpedienteService expedienteService;

    @Autowired
    private ServicioPlazoLegal servicioPlazoLegal;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    private Expediente expediente;

    @BeforeEach
    void prepararExpediente() {
        expediente = expedienteRepository.save(Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-CARGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .solicitanteId(UUID.randomUUID())
                .nombreTitular("Titular de Prueba de Carga")
                .documentoIdentidad("4256" + DIGITOS_DOCUMENTO)
                .nombreComercial("Establecimiento de Prueba")
                .giroNegocio("Comercio minorista")
                .direccionEstablecimiento("Jr. Lima 100, Ayacucho")
                .estado(EstadoExpediente.EN_EVALUACION)
                .fechaCreacion(LocalDateTime.now())
                .build());
    }

    @AfterEach
    void limpiar() {
        expedienteRepository.deleteById(expediente.getId());
    }

    @Test
    @DisplayName("150 consultas simultáneas de seguimiento público del expediente")
    void seguimientoPublicoConcurrente() throws InterruptedException {
        String numero = expediente.getNumeroTramite();
        ResultadoCarga resultado = ejecutarConcurrente(() -> numero.equals(
                expedienteService.obtenerParaSeguimientoPublico(numero, DIGITOS_DOCUMENTO).getNumeroTramite()));

        assertTrue(resultado.completado(), "Las consultas deben completarse antes del tiempo límite.");
        assertEquals(USUARIOS_CONCURRENTES, resultado.exitos(), "Todas las consultas deben resolverse correctamente.");
        assertTrue(resultado.latenciaMediaMs() < 3000.0, "La latencia media debe ser menor a 3000 ms.");
    }

    @Test
    @DisplayName("150 cálculos simultáneos del plazo legal con el calendario institucional")
    void calculoDePlazoConcurrente() throws InterruptedException {
        UUID id = expediente.getId();
        ResultadoCarga resultado = ejecutarConcurrente(() -> servicioPlazoLegal.calcular(id).getDiasPlazo() > 0);

        assertTrue(resultado.completado(), "Los cálculos deben completarse antes del tiempo límite.");
        assertEquals(USUARIOS_CONCURRENTES, resultado.exitos(), "Todos los cálculos deben resolverse correctamente.");
        assertTrue(resultado.latenciaMediaMs() < 3000.0, "La latencia media debe ser menor a 3000 ms.");
    }

    private record ResultadoCarga(boolean completado, int exitos, double latenciaMediaMs) {
    }

    private ResultadoCarga ejecutarConcurrente(Supplier<Boolean> consulta) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(USUARIOS_CONCURRENTES);
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(USUARIOS_CONCURRENTES);
        AtomicInteger exitos = new AtomicInteger();
        List<Long> latencias = new CopyOnWriteArrayList<>();

        for (int i = 0; i < USUARIOS_CONCURRENTES; i++) {
            executor.submit(() -> {
                try {
                    inicio.await();
                    Instant t0 = Instant.now();
                    boolean ok = consulta.get();
                    latencias.add(Duration.between(t0, Instant.now()).toMillis());
                    if (ok) {
                        exitos.incrementAndGet();
                    }
                } catch (Exception ignorada) {
                    // El fallo se refleja en el conteo de éxitos.
                } finally {
                    fin.countDown();
                }
            });
        }
        inicio.countDown();
        boolean completado = fin.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        double media = latencias.stream().mapToLong(Long::longValue).average().orElse(0.0);
        return new ResultadoCarga(completado, exitos.get(), media);
    }
}
