package com.example.loanaccount.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryLogDatabase {
    private final AtomicLong idSequence = new AtomicLong(1);
    private final ArrayDeque<AuditLogEntry> entries = new ArrayDeque<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final int maxEntries;

    public InMemoryLogDatabase(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be greater than zero");
        }
        this.maxEntries = maxEntries;
    }

    public AuditLogEntry insert(AuditLogEntry entry) {
        AuditLogEntry saved = new AuditLogEntry(
                idSequence.getAndIncrement(),
                entry.requestId(),
                entry.apiName(),
                entry.loggingStrategy(),
                entry.request(),
                entry.response(),
                entry.createdAt()
        );
        lock.writeLock().lock();
        try {
            if (entries.size() == maxEntries) {
                entries.removeFirst();
            }
            entries.addLast(saved);
        } finally {
            lock.writeLock().unlock();
        }
        return saved;
    }

    public List<AuditLogEntry> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(entries);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int capacity() {
        return maxEntries;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("[");
        List<AuditLogEntry> allEntries = findAll();
        for (int i = 0; i < allEntries.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(allEntries.get(i).toJson());
        }
        return json.append("]").toString();
    }
}
