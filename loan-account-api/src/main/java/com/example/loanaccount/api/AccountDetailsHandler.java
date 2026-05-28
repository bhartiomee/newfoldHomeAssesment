package com.example.loanaccount.api;

import com.example.loanaccount.logging.AuditLoggingService;
import com.example.loanaccount.model.ApiResponse;
import com.example.loanaccount.model.RequestSnapshot;
import com.example.loanaccount.service.AccountDetailsService;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.example.loanaccount.util.ValidationUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class AccountDetailsHandler implements HttpHandler {
    private final AccountDetailsService accountDetailsService;
    private final AuditLoggingService auditLoggingService;
    private final String defaultLoggingStrategy;

    public AccountDetailsHandler(
            AccountDetailsService accountDetailsService,
            AuditLoggingService auditLoggingService,
            String defaultLoggingStrategy
    ) {
        this.accountDetailsService = accountDetailsService;
        this.auditLoggingService = auditLoggingService;
        this.defaultLoggingStrategy = defaultLoggingStrategy;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpExchangeUtils.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        RequestSnapshot request = HttpExchangeUtils.snapshot(exchange);
        String logTo = defaultLoggingStrategy;
        int statusCode = 200;
        String responseBody;

        try {
            String accountId = request.queryParams().getOrDefault("accountId", "A1001");
            if (!ValidationUtils.isValidBankingId(accountId)) {
                statusCode = 400;
                responseBody = "{\"error\":\"Invalid accountId format\"}";
            } else {
                responseBody = accountDetailsService.getAccountDetails(accountId);
            }
        } catch (Exception exception) {
            statusCode = 500;
            responseBody = "{\"error\":\"" + HttpExchangeUtils.escapeJson(exception.getMessage()) + "\"}";
        }

        auditLoggingService.log(logTo, "account-details", request, new ApiResponse(statusCode, responseBody));
        HttpExchangeUtils.sendJson(exchange, statusCode, responseBody);
    }
}
