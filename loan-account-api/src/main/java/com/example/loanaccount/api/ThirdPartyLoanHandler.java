package com.example.loanaccount.api;

import com.example.loanaccount.util.HttpExchangeUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class ThirdPartyLoanHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpExchangeUtils.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        Map<String, String> query = HttpExchangeUtils.queryParams(exchange.getRequestURI().getRawQuery());
        String customerId = query.getOrDefault("customerId", "C001");
        String response = loanResponseFor(customerId);

        HttpExchangeUtils.sendJson(exchange, 200, response);
    }

    private String loanResponseFor(String customerId) {
        String escapedCustomerId = HttpExchangeUtils.escapeJson(customerId);
        return switch (customerId) {
            case "C001" -> baseLoanJson(escapedCustomerId, true, "APPROVED", 750000, 10.25, 60);
            case "C002" -> baseLoanJson(escapedCustomerId, true, "APPROVED", 250000, 11.75, 36);
            case "C003" -> baseLoanJson(escapedCustomerId, false, "REJECTED", 0, 0.00, 0);
            case "C004" -> baseLoanJson(escapedCustomerId, true, "PENDING_DOCUMENTS", 500000, 12.10, 48);
            case "C005" -> baseLoanJson(escapedCustomerId, true, "APPROVED", 2500000, 9.85, 120);
            default -> baseLoanJson(escapedCustomerId, true, "PRE_APPROVED", 100000, 13.50, 24);
        };
    }

    private String baseLoanJson(
            String customerId,
            boolean eligible,
            String decision,
            int approvedLimit,
            double interestRate,
            int tenureMonths
    ) {
        return "{"
                + "\"provider\":\"mock-loan-provider\","
                + "\"customerId\":\"" + customerId + "\","
                + "\"eligible\":" + eligible + ","
                + "\"decision\":\"" + decision + "\","
                + "\"approvedLimit\":" + approvedLimit + ","
                + "\"interestRate\":" + String.format("%.2f", interestRate) + ","
                + "\"tenureMonths\":" + tenureMonths + ","
                + "\"currency\":\"INR\""
                + "}";
    }
}
