package com.example.loanaccount.security;

import com.example.loanaccount.config.AppProperties;
import com.example.loanaccount.logging.StructuredLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InternalApiKeyAuth {
    private final String internalApiKey;

    public InternalApiKeyAuth(AppProperties properties) {
        this.internalApiKey = properties.internal().apiKey() == null ? "" : properties.internal().apiKey().trim();
        if (this.internalApiKey.isBlank()) {
            StructuredLogger.warn("internal_api_key_blank", Map.of(
                    "message", "Internal endpoints are open because app.internal.api-key is blank"
            ));
        }
    }

    public boolean isConfigured() {
        return !internalApiKey.isBlank();
    }

    public boolean isAuthorized(HttpServletRequest request) {
        if (!isConfigured()) {
            return true;
        }
        String providedKey = request.getHeader("X-Internal-Key");
        return internalApiKey.equals(providedKey);
    }
}
