package com.example.urlshortener.exception;

public class InvalidUrlException extends UrlShortenerException {
    public InvalidUrlException(String url) {
        super("Invalid URL: " + url);
    }
}
