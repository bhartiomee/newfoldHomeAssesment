package com.example.loanaccount.logging;

import com.example.loanaccount.util.HttpExchangeUtils;

import java.time.Instant;
import java.util.Map;

public final class StructuredLogger {
    private StructuredLogger() {
    }

    public static void info(String event, Map<String, String> fields) {
        log("INFO", event, fields);
    }

    public static void warn(String event, Map<String, String> fields) {
        log("WARN", event, fields);
    }

    public static void error(String event, Map<String, String> fields) {
        log("ERROR", event, fields);
    }

    private static void log(String level, String event, Map<String, String> fields) {
        StringBuilder json = new StringBuilder("{")
                .append("\"timestamp\":\"").append(Instant.now()).append("\",")
                .append("\"level\":\"").append(level).append("\",")
                .append("\"event\":\"").append(HttpExchangeUtils.escapeJson(event)).append("\"");

        for (Map.Entry<String, String> field : fields.entrySet()) {
            json.append(",\"")
                    .append(HttpExchangeUtils.escapeJson(field.getKey()))
                    .append("\":\"")
                    .append(HttpExchangeUtils.escapeJson(field.getValue()))
                    .append("\"");
        }

        json.append("}");
        System.out.println(json);
    }
}
