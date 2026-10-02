package pe.gob.munihuamanga.licencias.expedientes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración de ejecución asíncrona para el envío de notificaciones por correo electrónico.
 *
 * Fase 04 Sprint 4-B: Los métodos de NotificacionEmailService anotados con @Async
 * se ejecutan en este ThreadPool dedicado, para no bloquear el hilo HTTP que
 * atiende las peticiones del ciudadano o del funcionario.
 *
 * Dimensionamiento conservador para el volumen estimado de Huamanga:
 *   - Core:  2 hilos siempre disponibles
 *   - Max:   5 hilos en pico
 *   - Queue: 50 correos en cola (descartables si el SMTP no responde)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor dedicado para el envío asíncrono de correos.
     * Se referencia por nombre en @Async("emailExecutor") en NotificacionEmailService.
     */
    @Bean("emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-notif-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
