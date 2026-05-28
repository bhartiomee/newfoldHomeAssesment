package com.example.loanaccount.api;

import com.example.loanaccount.logging.StructuredLogger;
import com.example.loanaccount.resilience.CircuitBreaker;
import com.example.loanaccount.security.InternalApiKeyAuth;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class CircuitBreakerStatusHandler implements HttpHandler {
    private final CircuitBreaker circuitBreaker;
    private final InternalApiKeyAuth internalApiKeyAuth;

    public CircuitBreakerStatusHandler(CircuitBreaker circuitBreaker, InternalApiKeyAuth internalApiKeyAuth) {
        this.circuitBreaker = circuitBreaker;
        this.internalApiKeyAuth = internalApiKeyAuth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && "/circuit-breaker/status".equals(path)) {
            status(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && "/circuit-breaker/reset".equals(path)) {
            reset(exchange);
            return;
        }

        HttpExchangeUtils.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private void status(HttpExchange exchange) throws IOException {
        String response = "{"
                + "\"circuitBreaker\":\"" + circuitBreaker.name() + "\","
                + "\"state\":\"" + circuitBreaker.state().name() + "\","
                + "\"failureCount\":" + circuitBreaker.failureCount()
                + "}";
        HttpExchangeUtils.sendJson(exchange, 200, response);
    }

    private void reset(HttpExchange exchange) throws IOException {
        if (!internalApiKeyAuth.isAuthorized(exchange)) {
            HttpExchangeUtils.sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        circuitBreaker.reset();
        StructuredLogger.info("circuit_breaker_manual_reset", Map.of(
                "circuitBreaker", circuitBreaker.name(),
                "state", circuitBreaker.state().name()
        ));

        String response = "{"
                + "\"circuitBreaker\":\"" + circuitBreaker.name() + "\","
                + "\"state\":\"CLOSED\","
                + "\"action\":\"reset\""
                + "}";
        HttpExchangeUtils.sendJson(exchange, 200, response);
    }
}
