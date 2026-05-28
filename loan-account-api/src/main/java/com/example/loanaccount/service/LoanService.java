package com.example.loanaccount.service;

import com.example.loanaccount.model.LoanServiceResult;
import com.example.loanaccount.resilience.CircuitBreaker;

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
import java.util.Optional;

public class LoanService {
    private final String thirdPartyLoanUrl;
    private final HttpClient httpClient;
    private final RedisClient redisClient;
    private final CircuitBreaker circuitBreaker;
    private final int maxRetries;
    private final long cacheTtlSeconds;

    public LoanService(
            String thirdPartyLoanUrl,
            RedisClient redisClient,
            CircuitBreaker circuitBreaker,
            int maxRetries,
            long cacheTtlSeconds
    ) {
        this.thirdPartyLoanUrl = thirdPartyLoanUrl;
        this.redisClient = redisClient;
        this.circuitBreaker = circuitBreaker;
        this.maxRetries = maxRetries;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public LoanServiceResult getLoanDetails(String customerId) throws Exception {
        String cacheKey = "loan:customer:" + customerId;
        Optional<String> cachedResponse = redisClient.get(cacheKey);
        if (cachedResponse.isPresent()) {
            return new LoanServiceResult(cachedResponse.get(), 0, true);
        }

        RetryTrackingCall call = new RetryTrackingCall(customerId);
        String response;
        try {
            response = circuitBreaker.execute(call::callWithRetries);
        } catch (Exception exception) {
            throw new LoanServiceException(exception.getMessage(), call.retryCount(), exception);
        }
        redisClient.setEx(cacheKey, cacheTtlSeconds, response);
        return new LoanServiceResult(response, call.retryCount(), false);
    }

    private String callThirdParty(String customerId) throws IOException, InterruptedException {
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
