package com.example.aereopuerto.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/verify-2fa", "/api/auth/logout", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vuelos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/asientos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/config/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/mercadopago/webhook").permitAll()

                        .requestMatchers("/api/auth/completarPerfil", "/api/auth/toggle-2fa").hasAnyAuthority("ROLE_INCOMPLETO", "ROLE_USUARIO", "ROLE_EMPLEADO", "ROLE_ADMIN")

                        .requestMatchers(
                                HttpMethod.GET, "/api/reservas",
                                                "/api/reservas/filtrar",
                                                "/api/pasajes",
                                                "/api/pasajes/{id}",
                                                "/api/personas",
                                                "/api/personas/{id}",
                                                "/api/personas/filtrar"

                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST, "/api/vuelos",
                                                "/api/pasajes",
                                                "/api/equipajes",
                                                "/api/asistenciasAlViajero"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT, "/api/vuelos/{id}",
                                                "/api/personas/{id}",
                                                "/api/facturas/{id}",
                                                "/api/pasajes/{id}",
                                                "/api/equipajes/{id}",
                                                "/api/asistenciasAlViajero/{id}"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,"/api/reservas/{id}",
                                                  "/api/asistenciasAlViajero/{id}",
                                                  "/api/equipajes/{id}",
                                                  "/api/vuelos/{id}",
                                                  "/api/personas/{id}",
                                                  "/api/facturas/{id}",
                                                  "/api/pasajes/{id}"

                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/api/reservas/**",
                                "/api/personas/**",
                                "/api/pasajes/**",
                                "/api/favoritos/**",
                                "/api/facturas/**",
                                "/api/carrito/**",
                                "/api/compras/**",
                                "/api/asistenciasAlViajero/**",
                                "/api/auth/perfil",
                                "/api/equipajes"
                        ).hasAnyRole("USUARIO", "EMPLEADO", "ADMIN", "INCOMPLETO")

                        .requestMatchers(
                                "/api/aviones/**",
                                "/api/asignacion/**",
                                "/api/aeropuertos/**"
                        ).hasAnyRole("EMPLEADO", "ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", 401);
            body.put("error", "Unauthorized");
            body.put("message", "No autenticado");
            body.put("path", request.getRequestURI());
            body.put("timestamp", LocalDateTime.now().toString());
            response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", 403);
            body.put("error", "Forbidden");
            body.put("message", "No tenés permisos para acceder a este recurso");
            body.put("path", request.getRequestURI());
            body.put("timestamp", LocalDateTime.now().toString());
            response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:62079",
                "http://127.0.0.1:5173",
                "http://localhost:5173",
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "https://aerogest.ddns.net"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(List.of("x-auth-token"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
