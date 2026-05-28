package com.example.loanaccount.resilience;

import com.example.loanaccount.logging.StructuredLogger;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.functions.CheckedSupplier;

import java.time.Duration;
import java.util.Map;

public class LoanCircuitBreaker {
    private final CircuitBreaker circuitBreaker;

    public LoanCircuitBreaker(String name, int failureThreshold, long openDurationMillis) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be greater than zero");
        }

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Preserve the earlier "open after N consecutive failures" behavior.
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(failureThreshold)
                .minimumNumberOfCalls(failureThreshold)
                .failureRateThreshold(100.0f)
                .waitDurationInOpenState(Duration.ofMillis(openDurationMillis))
                .permittedNumberOfCallsInHalfOpenState(1)
                .build();

        this.circuitBreaker = CircuitBreaker.of(name, config);
        registerEventLogs();
    }

    public <T> T execute(CheckedSupplier<T> supplier) throws Exception {
        try {
            return CircuitBreaker.decorateCheckedSupplier(circuitBreaker, supplier).get();
        } catch (Throwable throwable) {
            if (throwable instanceof Exception exception) {
                throw exception;
            }
            throw new Exception(throwable);
        }
    }

    public void reset() {
        circuitBreaker.reset();
    }

    public void open() {
        circuitBreaker.transitionToOpenState();
    }

    public String name() {
        return circuitBreaker.getName();
    }

    public String state() {
        return circuitBreaker.getState().name();
    }

    public boolean isOpen() {
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    public long failedCalls() {
        return circuitBreaker.getMetrics().getNumberOfFailedCalls();
    }

    private void registerEventLogs() {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> StructuredLogger.warn("circuit_breaker_state_transition", Map.of(
                        "circuitBreaker", circuitBreaker.getName(),
                        "transition", event.getStateTransition().name()
                )))
                .onCallNotPermitted(event -> StructuredLogger.warn("circuit_breaker_call_not_permitted", Map.of(
                        "circuitBreaker", circuitBreaker.getName(),
                        "state", circuitBreaker.getState().name()
                )));
    }
}
