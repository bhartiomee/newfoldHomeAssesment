package com.example.urlshortener.repository.inmemory;

import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.exception.DuplicateShortKeyException;
import com.example.urlshortener.ports.UrlMappingRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryUrlMappingRepository implements UrlMappingRepository {
    private final ConcurrentMap<String, UrlMapping> byShortKey = new ConcurrentHashMap<>();

    @Override
    public UrlMapping save(UrlMapping mapping) {
        UrlMapping existing = byShortKey.putIfAbsent(mapping.shortKey(), mapping);
        if (existing != null) {
            throw new DuplicateShortKeyException(mapping.shortKey());
        }
        return mapping;
    }

    @Override
    public Optional<UrlMapping> findByShortKey(String shortKey) {
        return Optional.ofNullable(byShortKey.get(shortKey));
    }

    @Override
    public boolean existsByShortKey(String shortKey) {
        return byShortKey.containsKey(shortKey);
    }
}
