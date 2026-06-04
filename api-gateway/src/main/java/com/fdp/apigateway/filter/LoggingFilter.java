package com.fdp.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String START_TIME_KEY = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Log the incoming request details
        logger.info("Incoming Request: Method = [{}], URI = [{}], Path = [{}], Headers = [{}]",
                request.getMethod(), request.getURI(), request.getPath(), request.getHeaders());

        exchange.getAttributes().put(START_TIME_KEY, System.currentTimeMillis());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_KEY);
            ServerHttpResponse response = exchange.getResponse();
            
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                // Log the outgoing response details
                logger.info("Outgoing Response: URI = [{}], Status Code = [{}], Duration = [{}ms]",
                        request.getURI(), response.getStatusCode(), duration);
            } else {
                logger.info("Outgoing Response: URI = [{}], Status Code = [{}]",
                        request.getURI(), response.getStatusCode());
            }
        }));
    }

    @Override
    public int getOrder() {
        // High precedence to log requests as early as possible
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
