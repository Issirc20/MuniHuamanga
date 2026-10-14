package pe.gob.munihuamanga.licencias.expedientes.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

/**
 * Configuración central de seguridad Spring Security 6 con JWT (Fase 04 Sprint 4-C).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

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
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Archivos estáticos y páginas web públicas
                        .requestMatchers(
                                "/", "/index.html", "/portal-ciudadano.html",
                                "/formato-anexo1.html", "/portal-interno.html", "/verificar-licencia.html",
                                "/favicon.ico", "/css/**", "/js/**", "/img/**"
                        ).permitAll()

                        // 2. Endpoint público de autenticación
                        .requestMatchers("/api/auth/**").permitAll()

                        // 3. Verificación pública QR con minimización de datos (RNF-SEG-007 / CA-09)
                        // La vista ampliada requiere rol de INSPECTOR, EVALUADOR o ADMIN (CA-10)
                        .requestMatchers("/api/public/licencias/*/ampliada", "/api/public/verificar/*/ampliada")
                                .hasAnyRole("INSPECTOR", "EVALUADOR", "ADMIN")
                        .requestMatchers("/api/public/**").permitAll()

                        // 4. Seguimiento ciudadano público acotado (H07 / Ley N° 29733 - Datos ofuscados)
                        .requestMatchers("/api/expedientes/tramite/**").permitAll()

                        // 5. Mesa de partes virtual pública (registro de nueva solicitud)
                        .requestMatchers(HttpMethod.POST, "/api/expedientes").permitAll()

                        // 6. Formularios PDF bajo demanda en memoria sin ID (plantillas de preview)
                        .requestMatchers(HttpMethod.POST,
                                "/api/formularios/declaracion-jurada",
                                "/api/formularios/anexo1-declaracion-jurada",
                                "/api/formularios/anexo3-matriz-riesgo",
                                "/api/formularios/anexo3-matriz-riesgo-itse",
                                "/api/formularios/anexo4-condiciones",
                                "/api/formularios/anexo4-condiciones-seguridad",
                                "/api/formularios/voucher-sat").permitAll()

                        // 7. Tarifario TUPA: Consulta pública, Mantenimiento exclusivo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/tupa/**").permitAll()
                        .requestMatchers("/api/tupa/**").hasRole("ADMIN")

                        // 8. Documentación Swagger / OpenAPI y Health Check
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/h2-console/**").permitAll()

                        // 9. Integraciones y Adaptadores: Solo funcionarios autorizados
                        .requestMatchers("/api/integraciones/**").hasAnyRole("EVALUADOR", "ADMIN", "CAJERO")

                        // 10. Operaciones restringidas por rol funcional municipal (RBAC - H01)
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/pago", "/api/expedientes/*/registrar-pago").hasAnyRole("CAJERO", "ADMIN")
                        .requestMatchers("/api/expedientes/*/clasificar-riesgo", "/api/expedientes/*/clasificacion-riesgo").hasAnyRole("EVALUADOR", "ADMIN")
                        .requestMatchers("/api/expedientes/*/voucher", "/api/expedientes/*/generar-voucher").hasAnyRole("EVALUADOR", "CAJERO", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/aprobar", "/api/expedientes/*/emision-licencia").hasAnyRole("EVALUADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/rechazar", "/api/expedientes/*/observar").hasAnyRole("EVALUADOR", "ADMIN")
                        .requestMatchers("/api/formularios/licencia/**").hasAnyRole("EVALUADOR", "ADMIN")

                        // 11. Consultas y descargas de expedientes: Requieren autenticación y verificación a nivel de objeto (@PreAuthorize)
                        .requestMatchers(HttpMethod.GET, "/api/expedientes/**").authenticated()
                        .requestMatchers("/api/expedientes/**").authenticated()
                        .requestMatchers("/api/formularios/**").authenticated()

                        // 12. Regla Default-Deny: cualquier otra petición exige autenticación estricta (H05)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
