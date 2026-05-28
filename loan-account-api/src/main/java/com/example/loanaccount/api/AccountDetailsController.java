package com.example.loanaccount.api;

import com.example.loanaccount.config.LoggingStrategyProperties;
import com.example.loanaccount.logging.AuditLoggingService;
import com.example.loanaccount.model.AccountUpsertRequest;
import com.example.loanaccount.model.ApiResponse;
import com.example.loanaccount.model.RequestSnapshot;
import com.example.loanaccount.service.AccountDetailsService;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.example.loanaccount.util.ServletRequestUtils;
import com.example.loanaccount.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
public class AccountDetailsController {
    private final AccountDetailsService accountDetailsService;
    private final AuditLoggingService auditLoggingService;
    private final LoggingStrategyProperties loggingStrategyProperties;
    private final ExecutorService accountExecutor;

    public AccountDetailsController(
            AccountDetailsService accountDetailsService,
            AuditLoggingService auditLoggingService,
            LoggingStrategyProperties loggingStrategyProperties,
            @Qualifier("accountExecutor") ExecutorService accountExecutor
    ) {
        this.accountDetailsService = accountDetailsService;
        this.auditLoggingService = auditLoggingService;
        this.loggingStrategyProperties = loggingStrategyProperties;
        this.accountExecutor = accountExecutor;
    }

    @GetMapping(value = "/account-details", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> getAccountDetails(
            @RequestParam(defaultValue = "A1001") String accountId,
            HttpServletRequest servletRequest
    ) {
        RequestSnapshot request = ServletRequestUtils.snapshot(servletRequest);
        return CompletableFuture.supplyAsync(() -> handleAccountDetails(accountId, request), accountExecutor);
    }

    private ResponseEntity<String> handleAccountDetails(String accountId, RequestSnapshot request) {
        int statusCode = 200;
        String responseBody;

        try {
            if (!ValidationUtils.isValidBankingId(accountId)) {
                statusCode = 400;
                responseBody = "{\"error\":\"Invalid accountId format\"}";
            } else {
                responseBody = accountDetailsService.getAccountDetails(accountId);
            }
        } catch (Exception exception) {
            statusCode = 500;
            responseBody = "{\"error\":\"" + HttpExchangeUtils.escapeJson(exception.getMessage()) + "\"}";
        }

        String auditStrategy = auditLoggingService.log(
                loggingStrategyProperties.accountDetails(),
                "account-details",
                request,
                new ApiResponse(statusCode, responseBody)
        );
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Audit-Log-Strategy", auditStrategy)
                .body(responseBody);
    }

    @PostMapping(value = "/account-details", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> upsertAccount(
            @RequestBody AccountUpsertRequest body,
            HttpServletRequest servletRequest
    ) {
        RequestSnapshot request = ServletRequestUtils.snapshot(servletRequest);
        return CompletableFuture.supplyAsync(() -> handleAccountUpsert(body, request), accountExecutor);
    }

    private ResponseEntity<String> handleAccountUpsert(AccountUpsertRequest body, RequestSnapshot request) {
        int statusCode = 201;
        String responseBody;

        try {
            if (body == null || !ValidationUtils.isValidBankingId(body.accountId())) {
                statusCode = 400;
                responseBody = "{\"error\":\"Invalid accountId format\"}";
            } else {
                String accountJson = body.toAccountJson();
                accountDetailsService.saveAccountDetails(body.accountId(), accountJson);
                responseBody = "{\"message\":\"Account data saved in Redis\",\"redisKey\":\"account:"
                        + HttpExchangeUtils.escapeJson(body.accountId()) + "\",\"account\":" + accountJson + "}";
            }
        } catch (Exception exception) {
            statusCode = 500;
            responseBody = "{\"error\":\"" + HttpExchangeUtils.escapeJson(exception.getMessage()) + "\"}";
        }

        String auditStrategy = auditLoggingService.log(
                loggingStrategyProperties.accountDetails(),
                "account-details",
                request,
                new ApiResponse(statusCode, responseBody)
        );
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Audit-Log-Strategy", auditStrategy)
                .body(responseBody);
    }
}
