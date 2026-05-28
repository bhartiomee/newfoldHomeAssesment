package com.example.urlshortener.service;

public sealed interface RedirectResult permits RedirectResult.Found, RedirectResult.NotFound, RedirectResult.Expired, RedirectResult.NotActive {
    HttpStatus httpStatus();

    record Found(String longUrl, HttpStatus httpStatus) implements RedirectResult {
        public static Found temporaryRedirect(String longUrl) {
            return new Found(longUrl, HttpStatus.FOUND_302);
        }
    }

    record NotFound(String shortKey) implements RedirectResult {
        @Override
        public HttpStatus httpStatus() {
            return HttpStatus.NOT_FOUND_404;
        }
    }

    record Expired(String shortKey) implements RedirectResult {
        @Override
        public HttpStatus httpStatus() {
            return HttpStatus.GONE_410;
        }
    }

    record NotActive(String shortKey) implements RedirectResult {
        @Override
        public HttpStatus httpStatus() {
            return HttpStatus.GONE_410;
        }
    }
}
