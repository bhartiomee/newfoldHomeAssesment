package com.example.loanaccount.util;

import com.example.loanaccount.model.RequestSnapshot;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HttpExchangeUtils {
    private HttpExchangeUtils() {
    }

    public static RequestSnapshot snapshot(HttpExchange exchange) {
        Map<String, String> headers = singleValueHeaders(exchange.getRequestHeaders());
        String requestId = firstHeaderIgnoreCase(headers, "X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return new RequestSnapshot(
                requestId,
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                queryParams(exchange.getRequestURI().getRawQuery()),
                headers
        );
    }

    public static Map<String, String> queryParams(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int separatorIndex = pair.indexOf('=');
            String key = separatorIndex >= 0 ? pair.substring(0, separatorIndex) : pair;
            String value = separatorIndex >= 0 ? pair.substring(separatorIndex + 1) : "";
            params.put(decode(key), decode(value));
        }
        return params;
    }

    public static void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
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

    private static Map<String, String> singleValueHeaders(Headers headers) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                values.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return values;
    }

    private static String firstHeaderIgnoreCase(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
