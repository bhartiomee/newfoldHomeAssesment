package com.example.urlshortener.service;

import java.net.URI;
import java.util.Objects;

public final class ShortUrlBuilder {
    private final URI baseUri;

    public ShortUrlBuilder(String baseUrl) {
        this.baseUri = URI.create(Objects.requireNonNull(baseUrl, "baseUrl"));
    }

    public String build(String shortKey) {
        return baseUri.resolve("/" + shortKey).toString();
    }
}
