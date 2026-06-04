package com.fdp.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(-1)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected error occurred on the gateway.";

        if (ex instanceof ResponseStatusException responseStatusException) {
            status = HttpStatus.resolve(responseStatusException.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            message = responseStatusException.getReason();
        } else if (ex.getClass().getName().contains("ConnectException") || 
                   ex.getClass().getName().contains("TimeoutException")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service is temporarily unavailable. Please try again later.";
        }

        response.setStatusCode(status);

        logger.error("Gateway Error [Status: {}, Path: {}]: {}", 
                status, exchange.getRequest().getPath(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", Instant.now().toString());
        errorDetails.put("path", exchange.getRequest().getPath().value());
        errorDetails.put("status", status.value());
        errorDetails.put("error", status.getReasonPhrase());
        errorDetails.put("message", message);

        byte[] responseBytes;
        try {
            responseBytes = objectMapper.writeValueAsBytes(errorDetails);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize error details", e);
            responseBytes = "{\"message\":\"Internal Server Error\"}".getBytes();
        }

        DataBufferFactory bufferFactory = response.bufferFactory();
        DataBuffer buffer = bufferFactory.wrap(responseBytes);

        return response.writeWith(Mono.just(buffer));
    }
}
