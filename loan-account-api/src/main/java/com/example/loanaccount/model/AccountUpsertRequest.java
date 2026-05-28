package com.example.loanaccount.model;

import com.example.loanaccount.util.HttpExchangeUtils;

import java.math.BigDecimal;

public record AccountUpsertRequest(
        String accountId,
        String holderName,
        BigDecimal balance,
        String currency,
        String status
) {
    public String toAccountJson() {
        return "{"
                + "\"accountId\":\"" + HttpExchangeUtils.escapeJson(accountId) + "\","
                + "\"holderName\":\"" + HttpExchangeUtils.escapeJson(holderName) + "\","
                + "\"balance\":" + balance + ","
                + "\"currency\":\"" + HttpExchangeUtils.escapeJson(currency) + "\","
                + "\"status\":\"" + HttpExchangeUtils.escapeJson(status) + "\""
                + "}";
    }
}
