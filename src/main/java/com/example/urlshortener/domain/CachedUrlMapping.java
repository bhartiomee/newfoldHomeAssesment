package com.example.urlshortener.domain;

import java.time.Instant;
import java.util.Objects;

public record CachedUrlMapping(
        String shortKey,
        String longUrl,
        Instant expiresAt,
        MappingStatus status
) {
    public CachedUrlMapping {
        Objects.requireNonNull(shortKey, "shortKey");
        Objects.requireNonNull(longUrl, "longUrl");
        Objects.requireNonNull(status, "status");
    }

    public static CachedUrlMapping from(UrlMapping mapping) {
        return new CachedUrlMapping(
                mapping.shortKey(),
                mapping.longUrl(),
                mapping.expiresAt(),
                mapping.status()
        );
    }

    public boolean isExpiredAt(Instant instant) {
        return expiresAt != null && !expiresAt.isAfter(instant);
    }

    public boolean isRedirectableAt(Instant instant) {
        return status == MappingStatus.ACTIVE && !isExpiredAt(instant);
    }
}
