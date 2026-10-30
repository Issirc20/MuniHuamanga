package pe.gob.munihuamanga.licencias.expedientes.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtAccessDeniedHandler;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtAuthenticationEntryPoint;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad. En la capa de rutas solo se declaran los recursos públicos;
 * todo lo demás exige autenticación y se autoriza a nivel de método con {@code @PreAuthorize}
 * (roles institucionales y políticas de acceso por objeto).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PAGINAS_PUBLICAS = {
            "/", "/index.html", "/portal-ciudadano.html", "/formato-anexo1.html", "/portal-interno.html",
            "/verificar-licencia.html", "/verificar-correo.html", "/activar-acceso.html",
            "/favicon.ico", "/css/**", "/js/**", "/img/**"
    };

    private static final String[] FORMATOS_PUBLICOS = {
            "/api/formularios/anexo1-declaracion-jurada",
            "/api/formularios/declaracion-jurada",
            "/api/formularios/anexo1-itse-solicitud",
            "/api/formularios/anexo2-itse-riesgo",
            "/api/formularios/anexo3-matriz-riesgo-itse",
            "/api/formularios/anexo4-condiciones-seguridad"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final Environment environment;

    @Value("${munihuamanga.seguridad.origenes-permitidos:http://localhost:8081}")
    private String origenesPermitidos;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean produccion = environment.matchesProfiles("prod");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> {
                    if (produccion) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    } else {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PAGINAS_PUBLICAS).permitAll();

                    auth.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/renovar", "/api/auth/cerrar-sesion").permitAll();
                    auth.requestMatchers(HttpMethod.POST,
                            "/api/cuentas/registro",
                            "/api/cuentas/verificar-correo",
                            "/api/cuentas/verificacion/reenviar",
                            "/api/cuentas/accesos/activar").permitAll();

                    auth.requestMatchers(HttpMethod.GET, "/api/catalogo-ciiu/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/expedientes/tramite/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/licencias/verificar/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/plantillas-documentales", "/api/plantillas-documentales/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/tupa/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, FORMATOS_PUBLICOS).permitAll();

                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-ui.html").permitAll();
                    auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    auth.requestMatchers("/actuator/**").hasRole("ADMIN");

                    if (produccion) {
                        auth.requestMatchers("/h2-console/**").denyAll();
                    } else {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
