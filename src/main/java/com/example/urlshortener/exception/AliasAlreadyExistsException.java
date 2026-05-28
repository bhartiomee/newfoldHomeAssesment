package com.example.urlshortener.exception;

public class AliasAlreadyExistsException extends UrlShortenerException {
    public AliasAlreadyExistsException(String aliasKey) {
        super("Alias is already taken: " + aliasKey);
    }
}
