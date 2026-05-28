package com.example.loanaccount.logging;

public class DatabaseLogStrategy implements LogStrategy {
    private final InMemoryLogDatabase logDatabase;

    public DatabaseLogStrategy(InMemoryLogDatabase logDatabase) {
        this.logDatabase = logDatabase;
    }

    @Override
    public String name() {
        return "db";
    }

    @Override
    public void log(AuditLogEntry entry) {
        logDatabase.insert(entry);
    }
}
