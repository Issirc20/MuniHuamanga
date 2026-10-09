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
                                "/portal-interno.html", "/verificar-licencia.html",
                                "/favicon.ico", "/css/**", "/js/**", "/img/**"
                        ).permitAll()

                        // 2. Endpoint público de autenticación
                        .requestMatchers("/api/auth/**").permitAll()

                        // 3. Mesa de partes virtual pública (registro) y consulta de seguimiento acotada (H07)
                        .requestMatchers(HttpMethod.POST, "/api/expedientes").permitAll()
                        .requestMatchers("/api/expedientes/tramite/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // 4. Documentación Swagger / OpenAPI y Health Check
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/h2-console/**").permitAll()

                        // 5. Descargas de documentos ciudadanos
                        .requestMatchers("/api/expedientes/*/documentos/declaracion-jurada",
                                         "/api/expedientes/*/documentos/voucher-sat",
                                         "/api/expedientes/*/documentos/licencia",
                                         "/api/expedientes/*/qr").permitAll()

                        // 6. Formularios PDF generales (Anexos 1, 3, 4) y Licencia oficial (H08)
                        .requestMatchers("/api/formularios/declaracion-jurada",
                                         "/api/formularios/anexo3-matriz-riesgo",
                                         "/api/formularios/anexo4-condiciones",
                                         "/api/formularios/voucher-sat").permitAll()
                        .requestMatchers("/api/formularios/licencia/**").hasAnyRole("EVALUADOR", "ADMIN")

                        // 7. Tarifario TUPA: Consulta pública / funcionarios, Mantenimiento exclusivo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/tupa/**").permitAll()
                        .requestMatchers("/api/tupa/**").hasRole("ADMIN")

                        // 8. Integraciones y Adaptadores (H10): Solo funcionarios autorizados
                        .requestMatchers("/api/integraciones/**").hasAnyRole("EVALUADOR", "ADMIN", "CAJERO")

                        // 9. Operaciones restringidas por rol funcional municipal (RBAC - H01)
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/pago", "/api/expedientes/*/registrar-pago").hasAnyRole("CAJERO", "ADMIN")
                        .requestMatchers("/api/expedientes/*/clasificar-riesgo", "/api/expedientes/*/clasificacion-riesgo").hasAnyRole("EVALUADOR", "ADMIN")
                        .requestMatchers("/api/expedientes/*/voucher", "/api/expedientes/*/generar-voucher").hasAnyRole("EVALUADOR", "CAJERO", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/aprobar", "/api/expedientes/*/emision-licencia").hasAnyRole("EVALUADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/expedientes/*/rechazar", "/api/expedientes/*/observar").hasAnyRole("EVALUADOR", "ADMIN")

                        // 10. Consultas de expedientes internas protegidas por rol (H01)
                        .requestMatchers(HttpMethod.GET, "/api/expedientes/**").hasAnyRole("EVALUADOR", "ADMIN", "CAJERO")

                        // 11. Cualquier otra mutación de expedientes requiere autenticación
                        .requestMatchers("/api/expedientes/**").authenticated()

                        // 12. Regla Default-Deny: cualquier otra petición exige autenticación (H05)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
