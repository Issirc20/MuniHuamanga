package pe.gob.munihuamanga.licencias.formularios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración CORS global para el servicio-formularios.
 * Permite que el Portal Ciudadano (:8081) y el Portal Interno (:8081)
 * llamen directamente a los endpoints POST de generación de PDF sin ser
 * bloqueados por la política de Same-Origin del navegador.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:8081",
                                "http://localhost:8080",
                                "http://127.0.0.1:8081",
                                "http://127.0.0.1:8080"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Disposition", "Content-Type")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
