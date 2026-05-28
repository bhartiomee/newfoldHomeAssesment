package com.example.urlshortener.util;

import com.example.urlshortener.exception.InvalidUrlException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public final class UrlValidator {
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private UrlValidator() {
    }

    public static void requireValidHttpUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                throw new InvalidUrlException(url);
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException(url);
            }
        } catch (URISyntaxException ex) {
            throw new InvalidUrlException(url);
        }
    }
}
