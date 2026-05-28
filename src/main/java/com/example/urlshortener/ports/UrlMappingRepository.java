package com.example.urlshortener.ports;

import com.example.urlshortener.domain.UrlMapping;

import java.util.Optional;

public interface UrlMappingRepository {
    UrlMapping save(UrlMapping mapping);

    Optional<UrlMapping> findByShortKey(String shortKey);

    boolean existsByShortKey(String shortKey);
}
