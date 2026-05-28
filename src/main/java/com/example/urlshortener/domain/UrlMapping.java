package com.example.urlshortener.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record UrlMapping(
        long id,
        String shortKey,
        String longUrl,
        Long userId,
        Instant createdAt,
        Instant expiresAt,
        MappingStatus status
) {
    public UrlMapping {
        Objects.requireNonNull(shortKey, "shortKey");
        Objects.requireNonNull(longUrl, "longUrl");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(status, "status");
    }

    public Optional<Instant> expiry() {
        return Optional.ofNullable(expiresAt);
    }

    public boolean isExpiredAt(Instant instant) {
        return expiresAt != null && !expiresAt.isAfter(instant);
    }

    public boolean isRedirectableAt(Instant instant) {
        return status == MappingStatus.ACTIVE && !isExpiredAt(instant);
    }

    public UrlMapping withStatus(MappingStatus nextStatus) {
        return new UrlMapping(id, shortKey, longUrl, userId, createdAt, expiresAt, nextStatus);
    }
}
