package com.example.urlshortener.service;

import java.time.Instant;
import java.util.Optional;

public record CreateShortUrlRequest(
        String longUrl,
        Long userId,
        String customAlias,
        Instant expiresAt
) {
    public Optional<String> customAliasOptional() {
        return Optional.ofNullable(customAlias).filter(value -> !value.isBlank());
    }
}
