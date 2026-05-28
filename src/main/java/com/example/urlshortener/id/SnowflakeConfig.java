package com.example.urlshortener.id;

import java.time.Instant;

public record SnowflakeConfig(
        long customEpochMillis,
        long machineId
) {
    public static SnowflakeConfig singleDatacenter(long machineId) {
        return new SnowflakeConfig(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(), machineId);
    }
}
