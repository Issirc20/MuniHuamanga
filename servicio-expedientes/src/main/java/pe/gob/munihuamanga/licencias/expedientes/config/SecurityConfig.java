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
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Archivos estáticos y páginas web públicas
                        .requestMatchers(
                                "/", "/index.html", "/portal-ciudadano.html",
                                "/portal-interno.html", "/verificar-licencia.html",
                                "/favicon.ico", "/css/**", "/js/**", "/img/**"
                        ).permitAll()

                        // Endpoint público de autenticación
                        .requestMatchers("/api/auth/**").permitAll()

                        // Endpoints ciudadanos públicos (Mesa de partes virtual y seguimiento)
                        .requestMatchers(HttpMethod.POST, "/api/expedientes").permitAll()
                        .requestMatchers("/api/expedientes/tramite/**").permitAll()
                        .requestMatchers("/api/expedientes/*/documentos/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/formularios/**").permitAll()

                        // Documentación Swagger / OpenAPI y Monitorización Actuator / H2
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // Consultas de expedientes públicas y de lectura (Bandeja, métricas, seguimiento)
                        .requestMatchers(HttpMethod.GET, "/api/expedientes/**").permitAll()

                        // Operaciones restringidas por rol funcional municipal (RBAC)
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/pago", "/api/expedientes/*/registrar-pago").hasAnyRole("CAJERO", "ADMIN")
                        .requestMatchers("/api/expedientes/*/clasificar-riesgo", "/api/expedientes/*/clasificacion-riesgo").hasAnyRole("EVALUADOR", "ADMIN")
                        .requestMatchers("/api/expedientes/*/voucher", "/api/expedientes/*/generar-voucher").hasAnyRole("EVALUADOR", "CAJERO", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/aprobar", "/api/expedientes/*/emision-licencia").hasAnyRole("EVALUADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/rechazar", "/api/expedientes/*/observar").hasAnyRole("EVALUADOR", "ADMIN")

                        // Tarifario TUPA: Consulta pública / funcionarios, Mantenimiento exclusivo ADMIN (Sprint 4-D)
                        .requestMatchers(HttpMethod.GET, "/api/tupa/**").permitAll()
                        .requestMatchers("/api/tupa/**").hasRole("ADMIN")

                        // Cualquier otra mutación de expedientes requiere autenticación
                        .requestMatchers("/api/expedientes/**").authenticated()

                        // Cualquier otra petición
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
