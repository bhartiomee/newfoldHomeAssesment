package com.example.loanaccount.service;

import com.example.loanaccount.config.AppProperties;
import org.springframework.stereotype.Component;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.util.Optional;

@Component
public class JedisRedisClient implements RedisClient {
    private final JedisPooled jedis;

    public JedisRedisClient(AppProperties properties) {
        AppProperties.Redis redis = properties.redis();
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(redis.timeoutMillis())
                .socketTimeoutMillis(redis.timeoutMillis())
                .build();
        this.jedis = new JedisPooled(new HostAndPort(redis.host(), redis.port()), clientConfig);
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
