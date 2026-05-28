package com.example.loanaccount.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public class AppConfig {
    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        Path configPath = Path.of("application.properties");

        if (Files.exists(configPath)) {
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load application.properties", exception);
            }
        }

        return new AppConfig(properties);
    }

    public int serverPort() {
        return getInt("server.port", 8080);
    }

    public int serverBacklog() {
        return getInt("server.backlog", 0);
    }

    public int dispatcherPoolSize() {
        return getInt("executor.dispatcher.poolSize", 4);
    }

    public int dispatcherQueueCapacity() {
        return getInt("executor.dispatcher.queueCapacity", 100);
    }

    public int loanPoolSize() {
        return getInt("executor.loan.poolSize", 10);
    }

    public int loanQueueCapacity() {
        return getInt("executor.loan.queueCapacity", 50);
    }

    public int accountPoolSize() {
        return getInt("executor.account.poolSize", 10);
    }

    public int accountQueueCapacity() {
        return getInt("executor.account.queueCapacity", 50);
    }

    public int internalPoolSize() {
        return getInt("executor.internal.poolSize", 4);
    }

    public int internalQueueCapacity() {
        return getInt("executor.internal.queueCapacity", 20);
    }

    public int shutdownDrainSeconds() {
        return getInt("executor.shutdownDrainSeconds", 5);
    }

    public int awaitTerminationSeconds() {
        return getInt("executor.awaitTerminationSeconds", 10);
    }

    public String defaultLoggingStrategy() {
        return getString("logging.defaultStrategy", "db");
    }

    public Path fileLogDirectory() {
        return Path.of(getString("logging.file.directory", "logs"));
    }

    public int dbLogMaxEntries() {
        return getInt("logging.db.maxEntries", 10000);
    }

    public String thirdPartyLoanUrl() {
        return getString("thirdParty.loan.url", "");
    }

    public int loanHttpMaxRetries() {
        return getInt("loan.http.max.retries", 2);
    }

    public long loanCacheTtlSeconds() {
        return getLong("loan.cache.ttl.seconds", 300);
    }

    public int circuitBreakerFailureThreshold() {
        return getInt("circuitBreaker.loan.failureThreshold", 3);
    }

    public long circuitBreakerOpenDurationMillis() {
        return getLong("circuitBreaker.loan.openDurationMillis", 30000);
    }

    public String redisHost() {
        return getString("redis.host", "localhost");
    }

    public int redisPort() {
        return getInt("redis.port", 6379);
    }

    public int redisTimeoutMillis() {
        return getInt("redis.timeoutMillis", 2000);
    }

    public String internalApiKey() {
        return getString("internal.api.key", "");
    }

    private int getInt(String key, int defaultValue) {
        String value = getString(key, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer config for " + key + ": " + value, exception);
        }
    }

    private long getLong(String key, long defaultValue) {
        String value = getString(key, Long.toString(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid long config for " + key + ": " + value, exception);
        }
    }

    private String getString(String key, String defaultValue) {
        String envValue = System.getenv(toEnvKey(key));
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return properties.getProperty(key, defaultValue).trim();
    }

    private String toEnvKey(String key) {
        return key.replace('.', '_').toUpperCase(Locale.ROOT);
    }
}
