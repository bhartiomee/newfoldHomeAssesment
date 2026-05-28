package com.example.loanaccount.model;

import com.example.loanaccount.util.HttpExchangeUtils;

import java.math.BigDecimal;

public record LoanProviderDataRequest(
        String customerId,
        boolean eligible,
        String decision,
        long approvedLimit,
        BigDecimal interestRate,
        int tenureMonths,
        String currency
) {
    public String toLoanJson() {
        return "{"
                + "\"provider\":\"redis-mock-loan-provider\","
                + "\"customerId\":\"" + HttpExchangeUtils.escapeJson(customerId) + "\","
                + "\"eligible\":" + eligible + ","
                + "\"decision\":\"" + HttpExchangeUtils.escapeJson(decision) + "\","
                + "\"approvedLimit\":" + approvedLimit + ","
                + "\"interestRate\":" + interestRate + ","
                + "\"tenureMonths\":" + tenureMonths + ","
                + "\"currency\":\"" + HttpExchangeUtils.escapeJson(currency) + "\""
                + "}";
    }
}
