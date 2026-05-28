package com.example.urlshortener.util;

import com.example.urlshortener.exception.InvalidAliasException;

import java.util.regex.Pattern;

public final class AliasValidator {
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{2,63}$");

    private AliasValidator() {
    }

    public static void requireValidAlias(String aliasKey) {
        if (aliasKey == null || !ALIAS_PATTERN.matcher(aliasKey).matches()) {
            throw new InvalidAliasException(String.valueOf(aliasKey));
        }
    }
}
