package com.example.loanaccount.logging;

import com.example.loanaccount.model.ApiResponse;
import com.example.loanaccount.model.RequestSnapshot;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.example.loanaccount.util.PiiMaskingUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record AuditLogEntry(
        long id,
        String requestId,
        String apiName,
        String loggingStrategy,
        RequestSnapshot request,
        ApiResponse response,
        Instant createdAt
) {
    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"requestId\":\"" + HttpExchangeUtils.escapeJson(requestId) + "\","
                + "\"apiName\":\"" + HttpExchangeUtils.escapeJson(apiName) + "\","
                + "\"loggingStrategy\":\"" + HttpExchangeUtils.escapeJson(loggingStrategy) + "\","
                + "\"method\":\"" + HttpExchangeUtils.escapeJson(request.method()) + "\","
                + "\"path\":\"" + HttpExchangeUtils.escapeJson(request.path()) + "\","
                + "\"query\":" + HttpExchangeUtils.mapToJson(maskedQuery()) + ","
                + "\"statusCode\":" + response.statusCode() + ","
                + "\"retryCount\":" + response.retryCount() + ","
                + "\"cacheHit\":" + response.cacheHit() + ","
                + "\"response\":" + HttpExchangeUtils.safeJsonBody(PiiMaskingUtils.maskBalance(response.body())) + ","
                + "\"createdAt\":\"" + createdAt + "\""
                + "}";
    }

    private Map<String, String> maskedQuery() {
        Map<String, String> masked = new LinkedHashMap<>(request.queryParams());
        maskQueryValue(masked, "accountId");
        maskQueryValue(masked, "customerId");
        return masked;
    }

    private void maskQueryValue(Map<String, String> query, String key) {
        if (query.containsKey(key)) {
            query.put(key, PiiMaskingUtils.maskId(query.get(key)));
        }
    }
}
