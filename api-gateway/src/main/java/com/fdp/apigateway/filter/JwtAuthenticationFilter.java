package com.fdp.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String secret;
    private List<String> bypassUrls = new ArrayList<>();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getBypassUrls() {
        return bypassUrls;
    }

    public void setBypassUrls(List<String> bypassUrls) {
        this.bypassUrls = bypassUrls;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        logger.debug("Processing request path: {}", path);

        // Check if path is in the bypass list
        boolean isBypassed = bypassUrls.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        if (isBypassed) {
            logger.debug("Bypassing authentication for path: {}", path);
            return chain.filter(exchange);
        }

        // Validate Authorization Header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Authorization header is missing or malformed for path: {}", path);
            return handleException(exchange, HttpStatus.UNAUTHORIZED, "Authorization header is missing or invalid");
        }

        String token = authHeader.substring(7);
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = String.valueOf(claims.get("id"));
            String email = String.valueOf(claims.get("email"));
            String role = String.valueOf(claims.get("role"));

            logger.debug("Successfully validated token for user: {}, role: {}", email, role);

            // Forward identity and roles to downstream services via headers
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            logger.warn("JWT expired: {}", e.getMessage());
            return handleException(exchange, HttpStatus.UNAUTHORIZED, "Authentication token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT format: {}", e.getMessage());
            return handleException(exchange, HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
    }

    private Mono<Void> handleException(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", Instant.now().toString());
        errorDetails.put("path", exchange.getRequest().getPath().value());
        errorDetails.put("status", status.value());
        errorDetails.put("error", status.getReasonPhrase());
        errorDetails.put("message", message);

        byte[] responseBytes;
        try {
            responseBytes = objectMapper.writeValueAsBytes(errorDetails);
        } catch (Exception e) {
            logger.error("Failed to serialize auth error details", e);
            responseBytes = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBufferFactory bufferFactory = response.bufferFactory();
        DataBuffer buffer = bufferFactory.wrap(responseBytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run after LoggingFilter but before routing to downstream services
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
