package com.example.loanaccount.util;

import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern BANKING_ID_PATTERN = Pattern.compile("^[A-Z0-9]{1,20}$");

    private ValidationUtils() {
    }

    public static boolean isValidBankingId(String value) {
        return value != null && BANKING_ID_PATTERN.matcher(value).matches();
    }
}
