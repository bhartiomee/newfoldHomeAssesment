package com.example.urlshortener.id;

import com.example.urlshortener.exception.ClockMovedBackwardsException;

import java.time.Clock;
import java.util.Objects;

public final class SnowflakeIdGenerator implements IdGenerator {
    private static final int SEQUENCE_BITS = 12;
    private static final int MACHINE_ID_BITS = 10;

    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1;

    private static final int MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private final long customEpochMillis;
    private final long machineId;
    private final Clock clock;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(SnowflakeConfig config, Clock clock) {
        Objects.requireNonNull(config, "config");
        if (config.machineId() < 0 || config.machineId() > MAX_MACHINE_ID) {
            throw new IllegalArgumentException("machineId must be between 0 and " + MAX_MACHINE_ID);
        }
        this.customEpochMillis = config.customEpochMillis();
        this.machineId = config.machineId();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static SnowflakeIdGenerator singleDatacenter(long machineId) {
        return new SnowflakeIdGenerator(SnowflakeConfig.singleDatacenter(machineId), Clock.systemUTC());
    }

    @Override
    public synchronized long nextId() {
        long timestamp = currentMillis();
        if (timestamp < lastTimestamp) {
            throw new ClockMovedBackwardsException(lastTimestamp, timestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - customEpochMillis) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    private long waitUntilNextMillis(long previousTimestamp) {
        long timestamp = currentMillis();
        while (timestamp <= previousTimestamp) {
            Thread.onSpinWait();
            timestamp = currentMillis();
        }
        return timestamp;
    }

    private long currentMillis() {
        return clock.millis();
    }
}
