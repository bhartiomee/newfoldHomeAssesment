package com.example.loanaccount.api;

import com.example.loanaccount.logging.InMemoryLogDatabase;
import com.example.loanaccount.logging.StructuredLogger;
import com.example.loanaccount.model.ApiResponse;
import com.example.loanaccount.model.RequestSnapshot;
import com.example.loanaccount.resilience.LoanCircuitBreaker;
import com.example.loanaccount.security.InternalApiKeyAuth;
import com.example.loanaccount.logging.AuditLogEntry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
public class InternalController {
    private final InMemoryLogDatabase logDatabase;
    private final InternalApiKeyAuth internalApiKeyAuth;
    private final LoanCircuitBreaker circuitBreaker;
    private final Instant startTime;
    private final Clock clock;
    private final ExecutorService internalExecutor;

    public InternalController(
            InMemoryLogDatabase logDatabase,
            InternalApiKeyAuth internalApiKeyAuth,
            LoanCircuitBreaker circuitBreaker,
            Instant startTime,
            Clock clock,
            @Qualifier("internalExecutor") ExecutorService internalExecutor
    ) {
        this.logDatabase = logDatabase;
        this.internalApiKeyAuth = internalApiKeyAuth;
        this.circuitBreaker = circuitBreaker;
        this.startTime = startTime;
        this.clock = clock;
        this.internalExecutor = internalExecutor;
    }

    @GetMapping(value = "/logs/db", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> dbLogs(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (!internalApiKeyAuth.isAuthorized(request)) {
                return json(401, "{\"error\":\"Unauthorized\"}");
            }
            return json(200, logDatabase.toJson());
        }, internalExecutor);
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> health() {
        return CompletableFuture.supplyAsync(() -> {
            String status = circuitBreaker.isOpen() ? "DEGRADED" : "UP";
            String response = "{"
                    + "\"status\":\"" + status + "\","
                    + "\"checks\":{"
                    + "\"circuitBreaker\":\"" + circuitBreaker.state() + "\","
                    + "\"auditLogDb\":{\"size\":" + logDatabase.size() + ",\"capacity\":" + logDatabase.capacity() + "},"
                    + "\"uptime\":\"" + Duration.between(startTime, Instant.now(clock)) + "\""
                    + "}"
                    + "}";
            return json(200, response);
        }, internalExecutor);
    }

    @GetMapping(value = "/circuit-breaker/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> circuitBreakerStatus() {
        return CompletableFuture.supplyAsync(() -> {
            String response = "{"
                    + "\"circuitBreaker\":\"" + circuitBreaker.name() + "\","
                    + "\"state\":\"" + circuitBreaker.state() + "\","
                    + "\"failedCalls\":" + circuitBreaker.failedCalls()
                    + "}";
            return json(200, response);
        }, internalExecutor);
    }

    @PostMapping(value = "/circuit-breaker/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> resetCircuitBreaker(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (!internalApiKeyAuth.isAuthorized(request)) {
                return json(401, "{\"error\":\"Unauthorized\"}");
            }

            circuitBreaker.reset();
            StructuredLogger.info("circuit_breaker_manual_reset", Map.of(
                    "circuitBreaker", circuitBreaker.name(),
                    "state", circuitBreaker.state()
            ));
            return json(200, "{\"circuitBreaker\":\"" + circuitBreaker.name() + "\",\"state\":\"CLOSED\",\"action\":\"reset\"}");
        }, internalExecutor);
    }

    @PostMapping(value = "/circuit-breaker/open", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> openCircuitBreaker(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (!internalApiKeyAuth.isAuthorized(request)) {
                return json(401, "{\"error\":\"Unauthorized\"}");
            }

            circuitBreaker.open();
            StructuredLogger.warn("circuit_breaker_manual_open", Map.of(
                    "circuitBreaker", circuitBreaker.name(),
                    "state", circuitBreaker.state()
            ));
            return json(200, "{\"circuitBreaker\":\"" + circuitBreaker.name() + "\",\"state\":\"OPEN\",\"action\":\"open\"}");
        }, internalExecutor);
    }

    @PostMapping(value = "/logs/db/test-entry", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> insertDbLogTestEntry(
            HttpServletRequest request,
            @RequestParam(defaultValue = "TEST1") String customerId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (!internalApiKeyAuth.isAuthorized(request)) {
                return json(401, "{\"error\":\"Unauthorized\"}");
            }

            AuditLogEntry saved = logDatabase.insert(new AuditLogEntry(
                    0,
                    "manual-db-log-" + Instant.now(clock).toEpochMilli(),
                    "manual-test",
                    "db",
                    new RequestSnapshot(
                            "manual-db-log",
                            "POST",
                            "/logs/db/test-entry",
                            Map.of("customerId", customerId),
                            Map.of("source", "manual-test")
                    ),
                    new ApiResponse(200, "{\"message\":\"Manual DB audit log entry\"}"),
                    Instant.now(clock)
            ));

            return json(201, "{\"message\":\"Audit log entry written to in-memory DB\",\"entry\":" + saved.toJson() + "}");
        }, internalExecutor);
    }

    private ResponseEntity<String> json(int statusCode, String body) {
        return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
