package com.example.loanaccount.api;

import com.example.loanaccount.logging.InMemoryLogDatabase;
import com.example.loanaccount.security.InternalApiKeyAuth;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class DbLogsHandler implements HttpHandler {
    private final InMemoryLogDatabase logDatabase;
    private final InternalApiKeyAuth internalApiKeyAuth;

    public DbLogsHandler(InMemoryLogDatabase logDatabase, InternalApiKeyAuth internalApiKeyAuth) {
        this.logDatabase = logDatabase;
        this.internalApiKeyAuth = internalApiKeyAuth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpExchangeUtils.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        if (!internalApiKeyAuth.isAuthorized(exchange)) {
            HttpExchangeUtils.sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        HttpExchangeUtils.sendJson(exchange, 200, logDatabase.toJson());
    }
}
