package com.example.loanaccount.model;

public record LoanServiceResult(String body, int retryCount, boolean cacheHit) {
}
