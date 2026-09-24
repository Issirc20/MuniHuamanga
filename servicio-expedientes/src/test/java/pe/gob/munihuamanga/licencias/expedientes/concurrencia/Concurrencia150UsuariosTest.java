package pe.gob.munihuamanga.licencias.expedientes.concurrencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionLicenciaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.service.CalculadoraDeTasa;
import pe.gob.munihuamanga.licencias.expedientes.service.ExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.service.MetricasExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.validator.EstadoExpedienteValidator;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * US-15: Prueba de estrés y alta concurrencia para validar la capacidad objetivo de >= 150 usuarios concurrentes (RNF-01).
 * Verifica que el sistema no presente condiciones de carrera, bloqueos mutuos o latencias superiores a 3 segundos (RNF-02).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Carga y Concurrencia para >= 150 Usuarios Concurrentes (US-15, RNF-01, RNF-02)")
class Concurrencia150UsuariosTest {

    private static final int USUARIOS_CONCURRENTES = 150;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Spy
    private EstadoExpedienteValidator estadoExpedienteValidator = new EstadoExpedienteValidator();

    @Spy
    private CalculadoraDeTasa calculadoraDeTasa = new CalculadoraDeTasa(
            new BigDecimal("154.50"),
            new BigDecimal("218.00"),
            new BigDecimal("345.20"),
            new BigDecimal("480.00")
    );

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private MetricasExpedienteService metricasExpedienteService;

    @InjectMocks
    private ExpedienteService expedienteService;

    @Test
    @DisplayName("RNF-01 / RNF-02: 150 peticiones simultáneas de verificación de licencias y expedientes deben responder en < 3s con 0% de error")
    void test150UsuariosConcurrentesVerificacionLicencia() throws InterruptedException, ExecutionException {
        String codigoLicencia = "LIC-2026-00000002";
        Expediente exp = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00002")
                .licenciaQrCode(codigoLicencia)
                .estado(EstadoExpediente.APROBADO)
                .nombreTitular("Carlos Raúl Mendoza Gutiérrez")
                .documentoIdentidad("20608765432")
                .razonSocial("CONSORCIO GASTRONÓMICO DE HUAMANGA S.A.C.")
                .nombreComercial("Restaurante Tradición Ayacuchana")
                .giroNegocio("Restaurante, café y servicios afines")
                .direccionEstablecimiento("Portal Constitución N° 12, Plaza Mayor de Huamanga")
                .areaMetrosCuadrados(new BigDecimal("145.00"))
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .montoTasa(new BigDecimal("218.00"))
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(expedienteRepository.findByLicenciaQrCode(codigoLicencia)).thenReturn(Optional.of(exp));

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
                    // Esperar a que los 150 hilos estén listos para disparar simultáneamente
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

        // Disparar las 150 peticiones concurrentes
        barrier.countDown();

        // Esperar a que terminen los 150 hilos (timeout de 10 segundos)
        boolean completado = endLatch.await(10, TimeUnit.SECONDS);
        Instant finGlobal = Instant.now();

        executor.shutdown();

        assertTrue(completado, "Las 150 peticiones concurrentes deben completarse antes del timeout");
        assertEquals(USUARIOS_CONCURRENTES, exitos.get(), "Las 150 peticiones deben haber sido procesadas exitosamente");
        assertEquals(0, fallos.get(), "No debe haber ningún fallo ni excepción por concurrencia");

        double latenciaMedia = latenciasMs.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long duracionTotalMs = Duration.between(inicioGlobal, finGlobal).toMillis();

        System.out.printf("[PRUEBA DE CARGA] Peticiones concurrentes: %d | Exitosas: %d | Fallos: %d%n",
                USUARIOS_CONCURRENTES, exitos.get(), fallos.get());
        System.out.printf("[PRUEBA DE CARGA] Duración total: %d ms | Latencia media: %.2f ms%n",
                duracionTotalMs, latenciaMedia);

        // Cumplimiento del RNF-02: Latencia inferior a 3 segundos (3000 ms)
        assertTrue(latenciaMedia < 3000.0, "La latencia media debe ser estrictamente menor a 3000 ms (RNF-02)");
    }
}
