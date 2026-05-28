package com.example.loanaccount.util;

import com.example.loanaccount.model.RequestSnapshot;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ServletRequestUtils {
    private ServletRequestUtils() {
    }

    public static RequestSnapshot snapshot(HttpServletRequest request) {
        Map<String, String> headers = headers(request);
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        return new RequestSnapshot(
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                queryParams(request),
                headers
        );
    }

    private static Map<String, String> queryParams(HttpServletRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, paramValues) -> {
            if (paramValues.length > 0) {
                values.put(key, paramValues[0]);
            }
        });
        return values;
    }

    private static Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        for (String name : Collections.list(names)) {
            values.put(name, request.getHeader(name));
        }
        return values;
    }
}
