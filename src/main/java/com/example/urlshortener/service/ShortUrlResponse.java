package com.example.urlshortener.service;

import java.time.Instant;

public record ShortUrlResponse(
        long urlId,
        String shortKey,
        String shortUrl,
        String longUrl,
        Instant expiresAt
) {
}
