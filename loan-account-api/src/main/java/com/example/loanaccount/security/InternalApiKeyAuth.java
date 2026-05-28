package com.example.loanaccount.security;

import com.example.loanaccount.logging.StructuredLogger;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

public class InternalApiKeyAuth {
    private final String internalApiKey;

    public InternalApiKeyAuth(String internalApiKey) {
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey.trim();
        if (this.internalApiKey.isBlank()) {
            StructuredLogger.warn("internal_api_key_blank", Map.of(
                    "message", "Internal endpoints are open because internal.api.key is blank"
            ));
        }
    }

    public boolean isConfigured() {
        return !internalApiKey.isBlank();
    }

    public boolean isAuthorized(HttpExchange exchange) {
        if (!isConfigured()) {
            return true;
        }
        String providedKey = exchange.getRequestHeaders().getFirst("X-Internal-Key");
        return internalApiKey.equals(providedKey);
    }
}
