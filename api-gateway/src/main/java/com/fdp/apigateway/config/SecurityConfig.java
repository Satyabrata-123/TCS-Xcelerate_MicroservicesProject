package com.fdp.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF since microservices use stateless JWT tokens
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // Disable default CORS to avoid conflicts with Gateway CORS configuration
            .cors(ServerHttpSecurity.CorsSpec::disable)
            // Allow all requests to pass Spring Security's filter chain
            // Our custom JwtAuthenticationFilter will handle actual authentication/authorization
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .build();
    }
}
