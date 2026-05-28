package com.example.urlshortener.service;

public enum HttpStatus {
    FOUND_302(302),
    NOT_FOUND_404(404),
    GONE_410(410);

    private final int code;

    HttpStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
