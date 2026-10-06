package com.tappazo.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration — stateless, JWT-based (Sección 9, 10, 11).
 *
 * Public routes:
 *   POST /auth/google  — Google ID Token exchange for Tappazo JWT
 *   GET  /actuator/**  — health check (optional)
 *   WebSocket handshake /ws/**
 *
 * All other routes require a valid Tappazo Bearer JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless REST + WebSocket (token-based)
            .csrf(AbstractHttpConfigurer::disable)

            // No HTTP session — every request is authenticated via JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Route authorization rules
            .authorizeHttpRequests(auth -> auth
                // Auth endpoint — public (no JWT required)
                .requestMatchers(HttpMethod.POST, "/auth/google").permitAll()
                // WebSocket handshake — public (JWT is validated inside the WS handler)
                .requestMatchers("/ws/**").permitAll()
                // Actuator health — public
                .requestMatchers("/actuator/**").permitAll()
                // Everything else requires a valid JWT
                .anyRequest().authenticated())

            // Exception handling for unauthorized access
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "No autorizado: Token ausente o inválido")
                ))

            // Add our filter before the standard username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
