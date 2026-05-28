package com.example.urlshortener.exception;

public class CustomAliasNotAllowedException extends UrlShortenerException {
    public CustomAliasNotAllowedException(long userId) {
        super("Custom aliases require a paid plan for user: " + userId);
    }
}
