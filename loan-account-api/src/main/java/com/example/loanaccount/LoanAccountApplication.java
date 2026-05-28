package com.example.loanaccount;

import com.example.loanaccount.api.AccountDetailsHandler;
import com.example.loanaccount.api.CircuitBreakerStatusHandler;
import com.example.loanaccount.api.DbLogsHandler;
import com.example.loanaccount.api.ExecutorDispatchingHandler;
import com.example.loanaccount.api.HealthHandler;
import com.example.loanaccount.api.LoanHandler;
import com.example.loanaccount.api.ThirdPartyLoanHandler;
import com.example.loanaccount.config.AppConfig;
import com.example.loanaccount.logging.AuditLoggingService;
import com.example.loanaccount.logging.DatabaseLogStrategy;
import com.example.loanaccount.logging.FileSystemLogStrategy;
import com.example.loanaccount.logging.InMemoryLogDatabase;
import com.example.loanaccount.logging.LoggingStrategyResolver;
import com.example.loanaccount.logging.StructuredLogger;
import com.example.loanaccount.resilience.CircuitBreaker;
import com.example.loanaccount.security.InternalApiKeyAuth;
import com.example.loanaccount.service.AccountDetailsService;
import com.example.loanaccount.service.JedisRedisClient;
import com.example.loanaccount.service.LoanService;
import com.example.loanaccount.service.RedisClient;
import com.example.loanaccount.service.RedisSeeder;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoanAccountApplication {
    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        int port = config.serverPort();
        Clock clock = Clock.systemUTC();
        Instant startTime = Instant.now(clock);

        InMemoryLogDatabase logDatabase = new InMemoryLogDatabase(config.dbLogMaxEntries());
        CircuitBreaker loanCircuitBreaker = new CircuitBreaker(
                "loan-third-party",
                config.circuitBreakerFailureThreshold(),
                config.circuitBreakerOpenDurationMillis(),
                clock
        );
        InternalApiKeyAuth internalApiKeyAuth = new InternalApiKeyAuth(config.internalApiKey());

        LoggingStrategyResolver strategyResolver = new LoggingStrategyResolver()
                .withCircuitBreaker(loanCircuitBreaker)
                .register("file", new FileSystemLogStrategy(config.fileLogDirectory(), clock))
                .register("db", new DatabaseLogStrategy(logDatabase));

        AuditLoggingService auditLoggingService = new AuditLoggingService(strategyResolver, clock);
        RedisClient redisClient = new JedisRedisClient(config.redisHost(), config.redisPort(), config.redisTimeoutMillis());
        RedisSeeder.seedAccounts(redisClient);

        String thirdPartyLoanUrl = config.thirdPartyLoanUrl().isBlank()
                ? "http://localhost:" + port + "/third-party/loan"
                : config.thirdPartyLoanUrl();
        LoanService loanService = new LoanService(
                thirdPartyLoanUrl,
                redisClient,
                loanCircuitBreaker,
                config.loanHttpMaxRetries(),
                config.loanCacheTtlSeconds()
        );
        AccountDetailsService accountDetailsService = new AccountDetailsService(redisClient);

        ThreadPoolExecutor dispatcherPool = createPool("dispatcher-pool", config.dispatcherPoolSize(), config.dispatcherQueueCapacity());
        ThreadPoolExecutor loanPool = createPool("loan-pool", config.loanPoolSize(), config.loanQueueCapacity());
        ThreadPoolExecutor accountPool = createPool("account-pool", config.accountPoolSize(), config.accountQueueCapacity());
        ThreadPoolExecutor internalPool = createPool("internal-pool", config.internalPoolSize(), config.internalQueueCapacity());

        HttpServer server = HttpServer.create(new InetSocketAddress(port), config.serverBacklog());
        server.createContext("/loan", new ExecutorDispatchingHandler(
                new LoanHandler(loanService, auditLoggingService, config.defaultLoggingStrategy()), loanPool));
        server.createContext("/account-details", new ExecutorDispatchingHandler(
                new AccountDetailsHandler(accountDetailsService, auditLoggingService, config.defaultLoggingStrategy()), accountPool));
        server.createContext("/third-party/loan", new ExecutorDispatchingHandler(new ThirdPartyLoanHandler(), internalPool));
        server.createContext("/logs/db", new ExecutorDispatchingHandler(new DbLogsHandler(logDatabase, internalApiKeyAuth), internalPool));
        server.createContext("/health", new ExecutorDispatchingHandler(
                new HealthHandler(loanCircuitBreaker, logDatabase, startTime, clock), internalPool));
        CircuitBreakerStatusHandler circuitBreakerStatusHandler = new CircuitBreakerStatusHandler(loanCircuitBreaker, internalApiKeyAuth);
        server.createContext("/circuit-breaker/status", new ExecutorDispatchingHandler(circuitBreakerStatusHandler, internalPool));
        server.createContext("/circuit-breaker/reset", new ExecutorDispatchingHandler(circuitBreakerStatusHandler, internalPool));
        server.setExecutor(dispatcherPool);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(config.shutdownDrainSeconds());
            shutdownPools(config.awaitTerminationSeconds(), List.of(dispatcherPool, loanPool, accountPool, internalPool));
        }));

        server.start();

        StructuredLogger.info("application_started", Map.of(
                "port", Integer.toString(port),
                "redisHost", config.redisHost(),
                "defaultLoggingStrategy", config.defaultLoggingStrategy()
        ));
        System.out.println("Loan Account API started on http://localhost:" + port);
        System.out.println("Try: curl \"http://localhost:" + port + "/loan?customerId=C001\"");
        System.out.println("Try: curl \"http://localhost:" + port + "/account-details?accountId=A1001\"");
    }

    private static ThreadPoolExecutor createPool(String name, int size, int queueCapacity) {
        return new ThreadPoolExecutor(
                size,
                size,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                namedThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private static ThreadFactory namedThreadFactory(String poolName) {
        AtomicInteger sequence = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(poolName + "-" + sequence.getAndIncrement());
            return thread;
        };
    }

    private static void shutdownPools(long awaitTerminationSeconds, List<ThreadPoolExecutor> executors) {
        for (ThreadPoolExecutor executor : executors) {
            executor.shutdown();
        }
        for (ThreadPoolExecutor executor : executors) {
            try {
                if (!executor.awaitTermination(awaitTerminationSeconds, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }
}
