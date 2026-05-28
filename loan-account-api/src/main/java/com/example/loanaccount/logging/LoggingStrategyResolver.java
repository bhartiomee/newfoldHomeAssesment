package com.example.loanaccount.logging;

import com.example.loanaccount.resilience.CircuitBreaker;
import com.example.loanaccount.resilience.CircuitBreakerState;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoggingStrategyResolver {
    private final Map<String, LogStrategy> strategies = new HashMap<>();
    private CircuitBreaker circuitBreaker;

    public LoggingStrategyResolver withCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    public LoggingStrategyResolver register(String name, LogStrategy strategy) {
        strategies.put(name.toLowerCase(Locale.ROOT), strategy);
        return this;
    }

    public LogStrategy resolve(String name) {
        if (circuitBreaker != null && circuitBreaker.state() == CircuitBreakerState.OPEN) {
            return strategies.get("file");
        }

        String normalizedName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        LogStrategy strategy = strategies.get(normalizedName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported logging strategy: " + name + ". Use file or db.");
        }
        return strategy;
    }
}
