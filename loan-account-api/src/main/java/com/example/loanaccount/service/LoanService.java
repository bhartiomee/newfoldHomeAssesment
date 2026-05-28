package com.example.loanaccount.service;

import com.example.loanaccount.config.AppProperties;
import com.example.loanaccount.logging.StructuredLogger;
import com.example.loanaccount.model.LoanServiceResult;
import com.example.loanaccount.resilience.LoanCircuitBreaker;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
public class LoanService {
    private final String thirdPartyLoanUrl;
    private final HttpClient httpClient;
    private final RedisClient redisClient;
    private final LoanCircuitBreaker circuitBreaker;
    private final int maxRetries;
    private final long cacheTtlSeconds;

    public LoanService(
            AppProperties properties,
            RedisClient redisClient,
            LoanCircuitBreaker circuitBreaker
    ) {
        this.thirdPartyLoanUrl = properties.thirdParty().loan().url();
        this.redisClient = redisClient;
        this.circuitBreaker = circuitBreaker;
        this.maxRetries = properties.loan().http().maxRetries();
        this.cacheTtlSeconds = properties.loan().cacheTtlSeconds();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public LoanServiceResult getLoanDetails(String customerId) throws Exception {
        String cacheKey = "loan:customer:" + customerId;
        StructuredLogger.info("loan_cache_lookup_started", Map.of("cacheKey", cacheKey));
        Optional<String> cachedResponse = redisClient.get(cacheKey);
        if (cachedResponse.isPresent()) {
            StructuredLogger.info("loan_cache_hit", Map.of("cacheKey", cacheKey));
            return new LoanServiceResult(cachedResponse.get(), 0, true);
        }
        StructuredLogger.info("loan_cache_miss", Map.of("cacheKey", cacheKey));

        RetryTrackingCall call = new RetryTrackingCall(customerId);
        String response;
        try {
            // Retries run inside the supplier, so Resilience4j records only the final exhausted failure.
            StructuredLogger.info("loan_circuit_breaker_call_started", Map.of("customerId", customerId));
            response = circuitBreaker.execute(call::callWithRetries);
            StructuredLogger.info("loan_circuit_breaker_call_completed", Map.of("customerId", customerId));
        } catch (Exception exception) {
            throw new LoanServiceException(exception.getMessage(), call.retryCount(), exception);
        }
        StructuredLogger.info("loan_cache_write_started", Map.of("cacheKey", cacheKey));
        redisClient.setEx(cacheKey, cacheTtlSeconds, response);
        StructuredLogger.info("loan_cache_write_completed", Map.of("cacheKey", cacheKey));
        return new LoanServiceResult(response, call.retryCount(), false);
    }

    private String callThirdParty(String customerId) throws IOException, InterruptedException {
        StructuredLogger.info("loan_third_party_call_started", Map.of("customerId", customerId));
        String encodedCustomerId = URLEncoder.encode(customerId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofSeconds(5))
                .uri(URI.create(thirdPartyLoanUrl + "?customerId=" + encodedCustomerId))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Third-party loan provider failed with status " + response.statusCode());
        }
        StructuredLogger.info("loan_third_party_call_completed", Map.of(
                "customerId", customerId,
                "statusCode", Integer.toString(response.statusCode())
        ));
        return response.body();
    }

    private boolean isRetryable(Exception exception) {
        return exception instanceof ConnectException || exception instanceof HttpTimeoutException;
    }

    private final class RetryTrackingCall {
        private final String customerId;
        private int retryCount;

        private RetryTrackingCall(String customerId) {
            this.customerId = customerId;
        }

        private String callWithRetries() throws Exception {
            int attempt = 0;
            while (true) {
                try {
                    return callThirdParty(customerId);
                } catch (Exception exception) {
                    if (!isRetryable(exception) || attempt >= maxRetries) {
                        throw exception;
                    }
                    retryCount++;
                    long backoffMillis = 100L * (1L << attempt);
                    Thread.sleep(backoffMillis);
                    attempt++;
                }
            }
        }

        private int retryCount() {
            return retryCount;
        }
    }
}
