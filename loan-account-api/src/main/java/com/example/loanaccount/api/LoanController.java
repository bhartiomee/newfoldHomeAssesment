package com.example.loanaccount.api;

import com.example.loanaccount.config.LoggingStrategyProperties;
import com.example.loanaccount.logging.AuditLoggingService;
import com.example.loanaccount.logging.StructuredLogger;
import com.example.loanaccount.model.ApiResponse;
import com.example.loanaccount.model.LoanServiceResult;
import com.example.loanaccount.model.RequestSnapshot;
import com.example.loanaccount.service.LoanService;
import com.example.loanaccount.service.LoanServiceException;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.example.loanaccount.util.ServletRequestUtils;
import com.example.loanaccount.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
public class LoanController {
    private final LoanService loanService;
    private final AuditLoggingService auditLoggingService;
    private final LoggingStrategyProperties loggingStrategyProperties;
    private final ExecutorService loanExecutor;

    public LoanController(
            LoanService loanService,
            AuditLoggingService auditLoggingService,
            LoggingStrategyProperties loggingStrategyProperties,
            @Qualifier("loanExecutor") ExecutorService loanExecutor
    ) {
        this.loanService = loanService;
        this.auditLoggingService = auditLoggingService;
        this.loggingStrategyProperties = loggingStrategyProperties;
        this.loanExecutor = loanExecutor;
    }

    @GetMapping(value = "/loan", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> getLoan(
            @RequestParam(defaultValue = "C001") String customerId,
            HttpServletRequest servletRequest
    ) {
        RequestSnapshot request = ServletRequestUtils.snapshot(servletRequest);
        return CompletableFuture.supplyAsync(() -> handleLoan(customerId, request), loanExecutor);
    }

    private ResponseEntity<String> handleLoan(String customerId, RequestSnapshot request) {
        int statusCode = 200;
        String responseBody;
        int retryCount = 0;
        boolean cacheHit = false;

        try {
            if (!ValidationUtils.isValidBankingId(customerId)) {
                statusCode = 400;
                responseBody = "{\"error\":\"Invalid customerId format\"}";
            } else {
                StructuredLogger.info("loan_request_started", Map.of("requestId", request.requestId()));
                LoanServiceResult result = loanService.getLoanDetails(customerId);
                responseBody = result.body();
                retryCount = result.retryCount();
                cacheHit = result.cacheHit();
                StructuredLogger.info("loan_request_completed", Map.of(
                        "requestId", request.requestId(),
                        "cacheHit", Boolean.toString(cacheHit),
                        "retryCount", Integer.toString(retryCount)
                ));
            }
        } catch (LoanServiceException exception) {
            retryCount = exception.retryCount();
            statusCode = 500;
            responseBody = "{\"error\":\"" + HttpExchangeUtils.escapeJson(exception.getMessage()) + "\"}";
        } catch (Exception exception) {
            statusCode = 500;
            responseBody = "{\"error\":\"" + HttpExchangeUtils.escapeJson(exception.getMessage()) + "\"}";
        }

        String auditStrategy = auditLoggingService.log(
                loggingStrategyProperties.loan(),
                "loan",
                request,
                new ApiResponse(statusCode, responseBody, retryCount, cacheHit)
        );
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Audit-Log-Strategy", auditStrategy)
                .body(responseBody);
    }
}
