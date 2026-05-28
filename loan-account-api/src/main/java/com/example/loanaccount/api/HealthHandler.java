package com.example.loanaccount.api;

import com.example.loanaccount.logging.InMemoryLogDatabase;
import com.example.loanaccount.resilience.CircuitBreaker;
import com.example.loanaccount.resilience.CircuitBreakerState;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class HealthHandler implements HttpHandler {
    private final CircuitBreaker circuitBreaker;
    private final InMemoryLogDatabase logDatabase;
    private final Instant startTime;
    private final Clock clock;

    public HealthHandler(CircuitBreaker circuitBreaker, InMemoryLogDatabase logDatabase, Instant startTime, Clock clock) {
        this.circuitBreaker = circuitBreaker;
        this.logDatabase = logDatabase;
        this.startTime = startTime;
        this.clock = clock;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpExchangeUtils.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String circuitBreakerState = circuitBreaker.state().name();
        String status = circuitBreaker.state() == CircuitBreakerState.OPEN ? "DEGRADED" : "UP";
        String response = "{"
                + "\"status\":\"" + status + "\","
                + "\"checks\":{"
                + "\"circuitBreaker\":\"" + circuitBreakerState + "\","
                + "\"auditLogDb\":{\"size\":" + logDatabase.size() + ",\"capacity\":" + logDatabase.capacity() + "},"
                + "\"uptime\":\"" + Duration.between(startTime, Instant.now(clock)) + "\""
                + "}"
                + "}";

        HttpExchangeUtils.sendJson(exchange, 200, response);
    }
}
