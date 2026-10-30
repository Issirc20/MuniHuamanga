package pe.gob.munihuamanga.licencias.expedientes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Componentes de infraestructura compartidos: reloj institucional y ejecución programada.
 */
@Configuration
@EnableScheduling
public class InfraestructuraConfig {

    public static final ZoneId ZONA_HORARIA_INSTITUCIONAL = ZoneId.of("America/Lima");

    @Bean
    public Clock relojInstitucional() {
        return Clock.system(ZONA_HORARIA_INSTITUCIONAL);
    }
}
