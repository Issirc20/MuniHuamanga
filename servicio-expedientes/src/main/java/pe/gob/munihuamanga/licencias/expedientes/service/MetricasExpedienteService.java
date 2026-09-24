package pe.gob.munihuamanga.licencias.expedientes.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Servicio de métricas e instrumentación de observabilidad (US-14 / RNF-21).
 * Expone contadores y temporizadores de negocio en endpoints de Actuator y Prometheus.
 */
@Service
public class MetricasExpedienteService {

    private final Counter expedientesCreados;
    private final Counter expedientesAprobados;
    private final Counter expedientesRechazados;
    private final Counter consultasPublicas;
    private final Timer tiempoAtencionTramite;

    public MetricasExpedienteService(MeterRegistry registry) {
        this.expedientesCreados = Counter.builder("muni.expedientes.creados.total")
                .description("Total de solicitudes de licencia registradas en Mesa de Partes")
                .tag("servicio", "servicio-expedientes")
                .register(registry);

        this.expedientesAprobados = Counter.builder("muni.expedientes.aprobados.total")
                .description("Total de licencias de funcionamiento aprobadas y emitidas con código QR")
                .tag("servicio", "servicio-expedientes")
                .register(registry);

        this.expedientesRechazados = Counter.builder("muni.expedientes.rechazados.total")
                .description("Total de expedientes rechazados con resolución")
                .tag("servicio", "servicio-expedientes")
                .register(registry);

        this.consultasPublicas = Counter.builder("muni.licencias.consultas.publicas.total")
                .description("Total de consultas de autenticidad recibidas en el portal público RNF-20")
                .tag("servicio", "servicio-expedientes")
                .register(registry);

        this.tiempoAtencionTramite = Timer.builder("muni.expedientes.tiempo.atencion")
                .description("Tiempo empleado en procesar y evaluar expedientes")
                .tag("servicio", "servicio-expedientes")
                .register(registry);
    }

    public void registrarCreacion() {
        expedientesCreados.increment();
    }

    public void registrarAprobacion() {
        expedientesAprobados.increment();
    }

    public void registrarRechazo() {
        expedientesRechazados.increment();
    }

    public void registrarConsultaPublica() {
        consultasPublicas.increment();
    }

    public void registrarDuracionAtencion(long duracionMilis) {
        tiempoAtencionTramite.record(duracionMilis, TimeUnit.MILLISECONDS);
    }
}
