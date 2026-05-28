package com.example.urlshortener.exception;

public class DuplicateShortKeyException extends UrlShortenerException {
    public DuplicateShortKeyException(String shortKey) {
        super("Short key is already taken: " + shortKey);
    }
}
