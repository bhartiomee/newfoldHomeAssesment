package com.example.loanaccount.util;

public final class PiiMaskingUtils {
    private PiiMaskingUtils() {
    }

    public static String maskId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() == 1) {
            return value.charAt(0) + "***";
        }
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    public static String maskBalance(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        return json.replaceAll("(\"balance\"\\s*:\\s*)-?\\d+(\\.\\d+)?", "$1\"[MASKED]\"");
    }
}
