package com.example.loanaccount.model;

public record ApiResponse(int statusCode, String body, int retryCount, boolean cacheHit) {
    public ApiResponse(int statusCode, String body) {
        this(statusCode, body, 0, false);
    }
}
