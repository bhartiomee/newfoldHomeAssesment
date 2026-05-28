package com.example.urlshortener.domain;

import java.time.Instant;
import java.util.Objects;

public record Alias(
        long id,
        long userId,
        long urlId,
        String aliasKey,
        AliasStatus status,
        Instant createdAt,
        Instant expiredAt
) {
    public Alias {
        Objects.requireNonNull(aliasKey, "aliasKey");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
