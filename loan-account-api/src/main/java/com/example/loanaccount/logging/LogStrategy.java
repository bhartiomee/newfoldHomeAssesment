package com.example.loanaccount.logging;

public interface LogStrategy {
    String name();

    void log(AuditLogEntry entry) throws Exception;
}
