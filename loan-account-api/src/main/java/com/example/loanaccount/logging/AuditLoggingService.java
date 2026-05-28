package com.example.loanaccount.logging;

import com.example.loanaccount.model.ApiResponse;
import com.example.loanaccount.model.RequestSnapshot;

import java.time.Clock;
import java.time.Instant;

public class AuditLoggingService {
    private final LoggingStrategyResolver strategyResolver;
    private final Clock clock;

    public AuditLoggingService(LoggingStrategyResolver strategyResolver, Clock clock) {
        this.strategyResolver = strategyResolver;
        this.clock = clock;
    }

    public String log(String strategyName, String apiName, RequestSnapshot request, ApiResponse response) {
        try {
            LogStrategy strategy = strategyResolver.resolve(strategyName);
            AuditLogEntry entry = new AuditLogEntry(
                    0,
                    request.requestId(),
                    apiName,
                    strategy.name(),
                    request,
                    response,
                    Instant.now(clock)
            );
            strategy.log(entry);
            return strategy.name();
        } catch (Exception exception) {
            StructuredLogger.error("audit_logging_failed", java.util.Map.of(
                    "requestId", request.requestId(),
                    "strategy", strategyName,
                    "error", exception.getMessage()
            ));
            return "failed";
        }
    }
}
