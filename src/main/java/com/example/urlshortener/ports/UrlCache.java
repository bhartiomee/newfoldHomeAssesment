package com.example.urlshortener.ports;

import com.example.urlshortener.domain.CachedUrlMapping;

import java.time.Duration;
import java.util.Optional;

public interface UrlCache {
    Optional<CachedUrlMapping> get(String shortKey);

    void put(String shortKey, CachedUrlMapping mapping, Duration ttl);

    void evict(String shortKey);
}
