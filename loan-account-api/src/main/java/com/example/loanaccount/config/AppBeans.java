package com.example.loanaccount.config;

import com.example.loanaccount.logging.AuditLoggingService;
import com.example.loanaccount.logging.DatabaseLogStrategy;
import com.example.loanaccount.logging.FileSystemLogStrategy;
import com.example.loanaccount.logging.InMemoryLogDatabase;
import com.example.loanaccount.logging.LoggingStrategyResolver;
import com.example.loanaccount.resilience.LoanCircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AppBeans {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    Instant startTime(Clock clock) {
        return Instant.now(clock);
    }

    @Bean
    InMemoryLogDatabase inMemoryLogDatabase(AppProperties properties) {
        return new InMemoryLogDatabase(properties.logging().db().maxEntries());
    }

    @Bean
    LoanCircuitBreaker loanCircuitBreaker(AppProperties properties) {
        AppProperties.LoanCircuitBreaker config = properties.circuitBreaker().loan();
        return new LoanCircuitBreaker("loan-third-party", config.failureThreshold(), config.openDurationMillis());
    }

    @Bean
    LoggingStrategyResolver loggingStrategyResolver(
            AppProperties properties,
            Clock clock,
            InMemoryLogDatabase logDatabase,
            LoanCircuitBreaker loanCircuitBreaker
    ) {
        return new LoggingStrategyResolver()
                .withCircuitBreaker(loanCircuitBreaker)
                .register("file", new FileSystemLogStrategy(Path.of(properties.logging().fileDirectory()), clock))
                .register("db", new DatabaseLogStrategy(logDatabase));
    }

    @Bean
    AuditLoggingService auditLoggingService(LoggingStrategyResolver resolver, Clock clock) {
        return new AuditLoggingService(resolver, clock);
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService loanExecutor(AppProperties properties) {
        return pool("loan-pool", properties.executors().loan());
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService accountExecutor(AppProperties properties) {
        return pool("account-pool", properties.executors().account());
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService internalExecutor(AppProperties properties) {
        return pool("internal-pool", properties.executors().internal());
    }

    private ExecutorService pool(String name, AppProperties.Pool config) {
        return new ThreadPoolExecutor(
                config.poolSize(),
                config.poolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(config.queueCapacity()),
                namedThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private ThreadFactory namedThreadFactory(String poolName) {
        AtomicInteger sequence = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(poolName + "-" + sequence.getAndIncrement());
            return thread;
        };
    }
}
