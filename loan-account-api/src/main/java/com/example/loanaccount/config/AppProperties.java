package com.example.loanaccount.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Server server,
        Executors executors,
        Logging logging,
        Internal internal,
        ThirdParty thirdParty,
        Loan loan,
        CircuitBreaker circuitBreaker,
        Redis redis
) {
    public AppProperties {
        if (server == null) server = new Server(8080);
        if (executors == null) executors = new Executors(new Pool(10, 50), new Pool(10, 50), new Pool(4, 20));
        if (logging == null) logging = new Logging("db", "logs", new LogDb(10000));
        if (internal == null) internal = new Internal("");
        if (thirdParty == null) thirdParty = new ThirdParty(new ThirdPartyLoan("http://localhost:8080/third-party/loan"));
        if (loan == null) loan = new Loan(new LoanHttp(2), 300);
        if (circuitBreaker == null) circuitBreaker = new CircuitBreaker(new LoanCircuitBreaker(3, 30000));
        if (redis == null) redis = new Redis("localhost", 6379, 2000);
    }

    public record Server(int port) {
    }

    public record Executors(Pool loan, Pool account, Pool internal) {
    }

    public record Pool(int poolSize, int queueCapacity) {
    }

    public record Logging(String defaultStrategy, String fileDirectory, LogDb db) {
    }

    public record LogDb(int maxEntries) {
    }

    public record Internal(String apiKey) {
    }

    public record ThirdParty(ThirdPartyLoan loan) {
    }

    public record ThirdPartyLoan(String url) {
    }

    public record Loan(LoanHttp http, long cacheTtlSeconds) {
    }

    public record LoanHttp(int maxRetries) {
    }

    public record CircuitBreaker(LoanCircuitBreaker loan) {
    }

    public record LoanCircuitBreaker(int failureThreshold, long openDurationMillis) {
    }

    public record Redis(String host, int port, int timeoutMillis) {
    }
}
