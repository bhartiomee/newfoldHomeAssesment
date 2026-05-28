package com.example.loanaccount.api;

import com.example.loanaccount.logging.AuditLoggingService;
import com.example.loanaccount.model.ApiResponse;
import com.example.loanaccount.model.LoanServiceResult;
import com.example.loanaccount.model.RequestSnapshot;
import com.example.loanaccount.service.LoanServiceException;
import com.example.loanaccount.service.LoanService;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.example.loanaccount.util.ValidationUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class LoanHandler implements HttpHandler {
    private final LoanService loanService;
    private final AuditLoggingService auditLoggingService;
    private final String defaultLoggingStrategy;

    public LoanHandler(LoanService loanService, AuditLoggingService auditLoggingService, String defaultLoggingStrategy) {
        this.loanService = loanService;
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
        int retryCount = 0;
        boolean cacheHit = false;

        try {
            String customerId = request.queryParams().getOrDefault("customerId", "C001");
            if (!ValidationUtils.isValidBankingId(customerId)) {
                statusCode = 400;
                responseBody = "{\"error\":\"Invalid customerId format\"}";
            } else {
                LoanServiceResult result = loanService.getLoanDetails(customerId);
                responseBody = result.body();
                retryCount = result.retryCount();
                cacheHit = result.cacheHit();
            }
        } catch (LoanServiceException exception) {
            retryCount = exception.retryCount();
            statusCode = 500;
            responseBody = "{\"error\":\"" + HttpExchangeUtils.escapeJson(exception.getMessage()) + "\"}";
        } catch (Exception exception) {
            statusCode = 500;
            responseBody = "{\"error\":\"" + HttpExchangeUtils.escapeJson(exception.getMessage()) + "\"}";
        }

        auditLoggingService.log(logTo, "loan", request, new ApiResponse(statusCode, responseBody, retryCount, cacheHit));
        HttpExchangeUtils.sendJson(exchange, statusCode, responseBody);
    }
}
