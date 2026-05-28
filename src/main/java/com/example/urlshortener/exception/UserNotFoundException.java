package com.example.urlshortener.exception;

public class UserNotFoundException extends UrlShortenerException {
    public UserNotFoundException(long userId) {
        super("User not found: " + userId);
    }
}
