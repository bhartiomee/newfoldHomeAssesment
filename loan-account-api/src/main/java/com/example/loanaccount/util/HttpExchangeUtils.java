package com.example.loanaccount.util;

import java.util.Map;

public final class HttpExchangeUtils {
    private HttpExchangeUtils() {
    }

    public static String mapToJson(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (index > 0) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(entry.getKey())).append("\":")
                    .append("\"").append(escapeJson(entry.getValue())).append("\"");
            index++;
        }
        return json.append("}").toString();
    }

    public static String safeJsonBody(String body) {
        if (body == null) {
            return "null";
        }

        String trimmedBody = body.trim();
        if ((trimmedBody.startsWith("{") && trimmedBody.endsWith("}"))
                || (trimmedBody.startsWith("[") && trimmedBody.endsWith("]"))
                || "null".equals(trimmedBody)
                || "true".equals(trimmedBody)
                || "false".equals(trimmedBody)
                || trimmedBody.matches("-?\\d+(\\.\\d+)?")) {
            return trimmedBody;
        }

        return "\"" + escapeJson(body) + "\"";
    }

    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
