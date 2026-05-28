package com.example.loanaccount.service;

public class LoanServiceException extends Exception {
    private final int retryCount;

    public LoanServiceException(String message, int retryCount, Throwable cause) {
        super(message, cause);
        this.retryCount = retryCount;
    }

    public int retryCount() {
        return retryCount;
    }
}
