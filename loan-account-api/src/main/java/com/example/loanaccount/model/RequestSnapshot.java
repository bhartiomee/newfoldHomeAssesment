package com.example.loanaccount.model;

import java.util.Map;

public record RequestSnapshot(
        String requestId,
        String method,
        String path,
        Map<String, String> queryParams,
        Map<String, String> headers
) {
}
