package com.example.urlshortener.exception;

public class InvalidAliasException extends UrlShortenerException {
    public InvalidAliasException(String aliasKey) {
        super("Invalid alias: " + aliasKey);
    }
}
