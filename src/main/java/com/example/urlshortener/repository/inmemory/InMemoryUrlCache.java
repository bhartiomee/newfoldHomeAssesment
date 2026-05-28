package com.example.urlshortener.repository.inmemory;

import com.example.urlshortener.domain.CachedUrlMapping;
import com.example.urlshortener.ports.UrlCache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryUrlCache implements UrlCache {
    private final ConcurrentMap<String, CacheEntry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryUrlCache(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Optional<CachedUrlMapping> get(String shortKey) {
        CacheEntry entry = entries.get(shortKey);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(clock.instant())) {
            entries.remove(shortKey, entry);
            return Optional.empty();
        }
        return Optional.of(entry.mapping());
    }

    @Override
    public void put(String shortKey, CachedUrlMapping mapping, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            evict(shortKey);
            return;
        }
        entries.put(shortKey, new CacheEntry(mapping, clock.instant().plus(ttl)));
    }

    @Override
    public void evict(String shortKey) {
        entries.remove(shortKey);
    }

    private record CacheEntry(CachedUrlMapping mapping, Instant expiresAt) {
        private CacheEntry {
            Objects.requireNonNull(mapping, "mapping");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }
}
