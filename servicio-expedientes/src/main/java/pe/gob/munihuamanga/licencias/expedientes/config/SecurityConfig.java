package pe.gob.munihuamanga.licencias.expedientes.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtAccessDeniedHandler;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtAuthenticationEntryPoint;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtAuthenticationFilter;

import java.util.Arrays;

/**
 * Configuración central de seguridad Spring Security 6 con JWT (Fase 04 Sprint 4-C / Corrección I02.1).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final Environment environment;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> {
                    if (isProd) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    } else {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    // 1. Archivos estáticos y páginas web públicas
                    auth.requestMatchers(
                            "/", "/index.html", "/portal-ciudadano.html",
                            "/formato-anexo1.html", "/portal-interno.html", "/verificar-licencia.html",
                            "/favicon.ico", "/css/**", "/js/**", "/img/**"
                    ).permitAll();

                    // 2. Endpoint público de autenticación
                    auth.requestMatchers("/api/auth/**").permitAll();

                    // 3. Verificación pública QR con minimización de datos (RNF-SEG-007 / CA-09)
                    // La vista ampliada requiere rol de INSPECTOR, EVALUADOR o ADMIN (CA-10)
                    auth.requestMatchers("/api/public/licencias/*/ampliada", "/api/public/verificar/*/ampliada")
                            .hasAnyRole("INSPECTOR", "EVALUADOR", "ADMIN");
                    auth.requestMatchers("/api/public/**").permitAll();

                    // 4. Seguimiento ciudadano público acotado (H07 / Ley N° 29733 - Datos ofuscados)
                    auth.requestMatchers("/api/expedientes/tramite/**").permitAll();

                    // 5. Cuentas y Catálogo CIIU (Rutas públicas para registro y catálogo)
                    auth.requestMatchers(HttpMethod.POST, "/api/cuentas/registro").permitAll();
                    auth.requestMatchers("/api/cuentas/verificar-correo").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/catalogo-ciiu/**").permitAll();

                    // 6. Solicitudes Borrador y Reclamaciones (M03, M06)
                    auth.requestMatchers("/api/borradores/**").authenticated();
                    auth.requestMatchers("/api/expedientes/reclamaciones/**").authenticated();

                    // 7. Mesa de partes presencial física: Creación formal directa reservada para funcionarios (ADMIN, EVALUADOR).
                    // Los ciudadanos deben iniciar obligatoriamente mediante borradores (/api/borradores) y formalizar mediante (/api/borradores/{id}/presentar)
                    auth.requestMatchers(HttpMethod.POST, "/api/expedientes").hasAnyRole("ADMIN", "EVALUADOR");

                    // 7. Formularios PDF bajo demanda en memoria sin ID (plantillas de preview)
                    auth.requestMatchers(HttpMethod.POST,
                            "/api/formularios/declaracion-jurada",
                            "/api/formularios/anexo1-declaracion-jurada",
                            "/api/formularios/anexo3-matriz-riesgo",
                            "/api/formularios/anexo3-matriz-riesgo-itse",
                            "/api/formularios/anexo4-condiciones",
                            "/api/formularios/anexo4-condiciones-seguridad",
                            "/api/formularios/voucher-sat").permitAll();

                    // 7. Tarifario TUPA: Consulta pública, Mantenimiento exclusivo ADMIN
                    auth.requestMatchers(HttpMethod.GET, "/api/tupa/**").permitAll();
                    auth.requestMatchers("/api/tupa/**").hasRole("ADMIN");

                    // 8. Documentación Swagger / OpenAPI y Health Check
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                    auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    auth.requestMatchers("/actuator/**").hasRole("ADMIN");

                    // Restricción estricta de H2 Console (Corrección I02.1 / RNF-SEG-004):
                    // Denegada absolutamente en producción; habilitada solo en entornos de desarrollo/pruebas
                    if (isProd) {
                        auth.requestMatchers("/h2-console/**").denyAll();
                    } else {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }

                    // 9. Integraciones y Adaptadores: Solo funcionarios autorizados
                    auth.requestMatchers("/api/integraciones/**").hasAnyRole("EVALUADOR", "ADMIN", "CAJERO");

                    // 10. Operaciones restringidas por rol funcional municipal (RBAC - H01)
                    auth.requestMatchers(HttpMethod.POST, "/api/expedientes/*/pago", "/api/expedientes/*/registrar-pago").hasAnyRole("CAJERO", "ADMIN");
                    auth.requestMatchers("/api/expedientes/*/clasificar-riesgo", "/api/expedientes/*/clasificacion-riesgo").hasAnyRole("EVALUADOR", "ADMIN");
                    auth.requestMatchers("/api/expedientes/*/voucher", "/api/expedientes/*/generar-voucher").hasAnyRole("EVALUADOR", "CAJERO", "ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/expedientes/*/aprobar", "/api/expedientes/*/emision-licencia").hasAnyRole("EVALUADOR", "ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/expedientes/*/rechazar", "/api/expedientes/*/observar", "/api/expedientes/*/observaciones").hasAnyRole("EVALUADOR", "ADMIN");
                    auth.requestMatchers("/api/formularios/licencia/**").hasAnyRole("EVALUADOR", "ADMIN");

                    // 10.1 Validación Manual de Pagos SAT (M05 - I06)
                    auth.requestMatchers("/api/pagos/bandeja").hasAnyRole("CAJERO", "ADMIN");
                    auth.requestMatchers(HttpMethod.POST,
                            "/api/pagos/*/iniciar-revision",
                            "/api/pagos/*/validar-manual",
                            "/api/pagos/*/observar",
                            "/api/pagos/*/rechazar").hasAnyRole("CAJERO", "ADMIN");
                    auth.requestMatchers("/api/pagos/**").authenticated();

                    // 10.2 Evaluación de Zonificación y Compatibilidad de Uso (M07 - I06)
                    auth.requestMatchers("/api/zonificacion/bandeja").hasAnyRole("EVALUADOR", "ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/expedientes/*/zonificacion/evaluar").hasAnyRole("EVALUADOR", "ADMIN");
                    auth.requestMatchers("/api/zonificacion/**").authenticated();

                    // 10.3 Evaluación Administrativa e Informes Técnicos (M08 - I07)
                    auth.requestMatchers("/api/evaluaciones-administrativas/**").hasAnyRole("EVALUADOR", "ADMIN");
                    auth.requestMatchers("/api/rectificaciones-ubicacion/*/evaluar", "/api/rectificaciones-ubicacion/pendientes").hasAnyRole("EVALUADOR", "ADMIN");
                    auth.requestMatchers("/api/rectificaciones-ubicacion/**").authenticated();

                    // 10.4 Resoluciones Administrativas y Licencias Oficiales (M09 - I08)
                    auth.requestMatchers("/api/licencias/verificar/**").permitAll();
                    auth.requestMatchers("/api/inspector/**").hasAnyRole("INSPECTOR", "ADMIN");
                    auth.requestMatchers("/api/resoluciones/**").authenticated();
                    auth.requestMatchers("/api/licencias/**").authenticated();

                    // 11. Consultas y descargas de expedientes: Requieren autenticación y verificación a nivel de objeto (@PreAuthorize)
                    auth.requestMatchers(HttpMethod.GET, "/api/expedientes/**").authenticated();
                    auth.requestMatchers("/api/expedientes/**").authenticated();
                    auth.requestMatchers("/api/formularios/**").authenticated();

                    // 12. Regla Default-Deny: cualquier otra petición exige autenticación estricta (H05)
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
