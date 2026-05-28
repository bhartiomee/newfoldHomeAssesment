package com.example.loanaccount.resilience;

import com.example.loanaccount.logging.StructuredLogger;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;

public class CircuitBreaker {
    private final String name;
    private final int failureThreshold;
    private final long openDurationMillis;
    private final Clock clock;

    private CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private int failureCount;
    private Instant openedAt;

    public CircuitBreaker(String name, int failureThreshold, long openDurationMillis, Clock clock) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be greater than zero");
        }
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.openDurationMillis = openDurationMillis;
        this.clock = clock;
    }

    public <T> T execute(Callable<T> callable) throws Exception {
        ensureCallAllowed();

        try {
            T result = callable.call();
            onSuccess();
            return result;
        } catch (Exception exception) {
            onFailure(exception);
            throw exception;
        }
    }

    public synchronized void reset() {
        state = CircuitBreakerState.CLOSED;
        failureCount = 0;
        openedAt = null;
    }

    public synchronized CircuitBreakerState state() {
        refreshStateIfOpenWindowExpired();
        return state;
    }

    public synchronized int failureCount() {
        return failureCount;
    }

    public String name() {
        return name;
    }

    private synchronized void ensureCallAllowed() {
        refreshStateIfOpenWindowExpired();
        if (state == CircuitBreakerState.OPEN) {
            throw new CircuitBreakerOpenException("Circuit breaker is OPEN for " + name);
        }
    }

    private synchronized void onSuccess() {
        failureCount = 0;
        if (state != CircuitBreakerState.CLOSED) {
            state = CircuitBreakerState.CLOSED;
            openedAt = null;
        }
    }

    private synchronized void onFailure(Exception exception) {
        failureCount++;
        if (failureCount >= failureThreshold) {
            state = CircuitBreakerState.OPEN;
            openedAt = Instant.now(clock);
            StructuredLogger.warn("circuit_breaker_opened", Map.of(
                    "circuitBreaker", name,
                    "failureCount", Integer.toString(failureCount),
                    "error", exception.getClass().getSimpleName()
            ));
        }
    }

    private void refreshStateIfOpenWindowExpired() {
        if (state == CircuitBreakerState.OPEN
                && openedAt != null
                && Instant.now(clock).toEpochMilli() - openedAt.toEpochMilli() >= openDurationMillis) {
            state = CircuitBreakerState.CLOSED;
            failureCount = 0;
            openedAt = null;
            StructuredLogger.info("circuit_breaker_auto_reset", Map.of("circuitBreaker", name));
        }
    }
}
