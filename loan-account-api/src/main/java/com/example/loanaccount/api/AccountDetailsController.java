package com.example.loanaccount.api;

import com.example.loanaccount.config.AppProperties;
import com.example.loanaccount.logging.AuditLoggingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
public class AccountDetailsController {
    private final AccountDetailsService accountDetailsService;
    private final AuditLoggingService auditLoggingService;
    private final AppProperties properties;
    private final ExecutorService accountExecutor;

    public AccountDetailsController(
            AccountDetailsService accountDetailsService,
            AuditLoggingService auditLoggingService,
            AppProperties properties,
            @Qualifier("accountExecutor") ExecutorService accountExecutor
    ) {
        this.accountDetailsService = accountDetailsService;
        this.auditLoggingService = auditLoggingService;
        this.properties = properties;
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

        auditLoggingService.log(
                properties.logging().defaultStrategy(),
                "account-details",
                request,
                new ApiResponse(statusCode, responseBody)
        );
        return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(responseBody);
    }
}
