package com.example.loanaccount.service;

import java.util.Optional;

public interface RedisClient {
    Optional<String> get(String key);

    void set(String key, String value);

    void setEx(String key, long ttlSeconds, String value);
}
