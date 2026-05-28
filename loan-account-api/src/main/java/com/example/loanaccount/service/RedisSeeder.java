package com.example.loanaccount.service;

public final class RedisSeeder {
    private RedisSeeder() {
    }

    public static void seedAccounts(RedisClient redisClient) {
        redisClient.set("account:A1001", "{\"accountId\":\"A1001\",\"holderName\":\"Omee Bharti\",\"balance\":125000.50,\"currency\":\"INR\",\"status\":\"ACTIVE\"}");
        redisClient.set("account:A1002", "{\"accountId\":\"A1002\",\"holderName\":\"Demo User\",\"balance\":8800.00,\"currency\":\"INR\",\"status\":\"ACTIVE\"}");
        redisClient.set("account:A1003", "{\"accountId\":\"A1003\",\"holderName\":\"Priya Sharma\",\"balance\":45210.75,\"currency\":\"INR\",\"status\":\"ACTIVE\"}");
        redisClient.set("account:A1004", "{\"accountId\":\"A1004\",\"holderName\":\"Rahul Mehta\",\"balance\":0.00,\"currency\":\"INR\",\"status\":\"FROZEN\"}");
        redisClient.set("account:A1005", "{\"accountId\":\"A1005\",\"holderName\":\"Ananya Rao\",\"balance\":983450.20,\"currency\":\"INR\",\"status\":\"ACTIVE\"}");
        redisClient.set("account:A1006", "{\"accountId\":\"A1006\",\"holderName\":\"Karan Singh\",\"balance\":-2500.00,\"currency\":\"INR\",\"status\":\"OVERDRAWN\"}");
        redisClient.set("account:A1007", "{\"accountId\":\"A1007\",\"holderName\":\"Neha Verma\",\"balance\":15000.00,\"currency\":\"INR\",\"status\":\"DORMANT\"}");
    }
}
