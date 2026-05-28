package com.example.urlshortener.exception;

public class ClockMovedBackwardsException extends UrlShortenerException {
    public ClockMovedBackwardsException(long lastTimestamp, long currentTimestamp) {
        super("System clock moved backwards from " + lastTimestamp + " to " + currentTimestamp);
    }
}
