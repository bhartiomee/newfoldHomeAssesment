package com.example.loanaccount.service;

import com.example.loanaccount.util.HttpExchangeUtils;
import org.springframework.stereotype.Service;

@Service
public class AccountDetailsService {
    private final RedisClient redisClient;

    public AccountDetailsService(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public String getAccountDetails(String accountId) {
        return redisClient.get("account:" + accountId)
                .orElse("{\"error\":\"No account found for accountId "
                        + HttpExchangeUtils.escapeJson(accountId) + "\"}");
    }

    public void saveAccountDetails(String accountId, String accountJson) {
        redisClient.set("account:" + accountId, accountJson);
    }
}
