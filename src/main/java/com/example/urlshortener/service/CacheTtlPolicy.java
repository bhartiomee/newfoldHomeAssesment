package com.example.urlshortener.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class CacheTtlPolicy {
    private final Clock clock;
    private final Duration defaultTtl;
    private final Duration maxTtl;

    private CacheTtlPolicy(Clock clock, Duration defaultTtl, Duration maxTtl) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.defaultTtl = Objects.requireNonNull(defaultTtl, "defaultTtl");
        this.maxTtl = Objects.requireNonNull(maxTtl, "maxTtl");
    }

    public static Builder builder(Clock clock) {
        return new Builder(clock);
    }

    public Duration ttlFor(Instant expiresAt) {
        Duration ttl = expiresAt == null ? defaultTtl : Duration.between(clock.instant(), expiresAt);
        if (ttl.compareTo(maxTtl) > 0) {
            return maxTtl;
        }
        return ttl;
    }

    public static final class Builder {
        private final Clock clock;
        private Duration defaultCacheTtl = Duration.ofHours(6);
        private Duration maxCacheTtl = Duration.ofDays(1);

        private Builder(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
        }

        public Builder defaultCacheTtl(Duration defaultCacheTtl) {
            this.defaultCacheTtl = Objects.requireNonNull(defaultCacheTtl, "defaultCacheTtl");
            return this;
        }

        public Builder maxCacheTtl(Duration maxCacheTtl) {
            this.maxCacheTtl = Objects.requireNonNull(maxCacheTtl, "maxCacheTtl");
            return this;
        }

        public CacheTtlPolicy build() {
            return new CacheTtlPolicy(clock, defaultCacheTtl, maxCacheTtl);
        }
    }
}
