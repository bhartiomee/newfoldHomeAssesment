package com.example.loanaccount.service;

import redis.clients.jedis.JedisPooled;

import java.util.Optional;

public class JedisRedisClient implements RedisClient {
    private final JedisPooled jedis;

    public JedisRedisClient(String host, int port, int timeoutMillis) {
        this.jedis = new JedisPooled(host, port, timeoutMillis);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(jedis.get(key));
    }

    @Override
    public void set(String key, String value) {
        jedis.set(key, value);
    }

    @Override
    public void setEx(String key, long ttlSeconds, String value) {
        jedis.setex(key, ttlSeconds, value);
    }
}
