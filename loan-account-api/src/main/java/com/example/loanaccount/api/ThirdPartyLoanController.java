package com.example.loanaccount.api;

import com.example.loanaccount.config.LoggingStrategyProperties;
import com.example.loanaccount.logging.AuditLoggingService;
import com.example.loanaccount.model.ApiResponse;
import com.example.loanaccount.model.LoanProviderDataRequest;
import com.example.loanaccount.model.RequestSnapshot;
import com.example.loanaccount.service.RedisClient;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.example.loanaccount.util.ServletRequestUtils;
import com.example.loanaccount.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThirdPartyLoanController {
    private final RedisClient redisClient;
    private final AuditLoggingService auditLoggingService;
    private final LoggingStrategyProperties loggingStrategyProperties;

    public ThirdPartyLoanController(
            RedisClient redisClient,
            AuditLoggingService auditLoggingService,
            LoggingStrategyProperties loggingStrategyProperties
    ) {
        this.redisClient = redisClient;
        this.auditLoggingService = auditLoggingService;
        this.loggingStrategyProperties = loggingStrategyProperties;
    }

    @GetMapping(value = "/third-party/loan", produces = MediaType.APPLICATION_JSON_VALUE)
    public String loan(@RequestParam(defaultValue = "C001") String customerId) {
        String redisKey = redisLoanKey(customerId);
        String configuredResponse = redisClient.get(redisKey).orElse(null);
        if (configuredResponse != null) {
            return configuredResponse;
        }
        return loanResponseFor(customerId);
    }

    @PostMapping(value = "/third-party/loan-data", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> upsertLoanData(
            @RequestBody LoanProviderDataRequest body,
            HttpServletRequest servletRequest
    ) {
        RequestSnapshot request = ServletRequestUtils.snapshot(servletRequest);
        int statusCode = 201;
        String responseBody;

        try {
            if (body == null || !ValidationUtils.isValidBankingId(body.customerId())) {
                statusCode = 400;
                responseBody = "{\"error\":\"Invalid customerId format\"}";
            } else {
                String redisKey = redisLoanKey(body.customerId());
                String loanJson = body.toLoanJson();
                redisClient.set(redisKey, loanJson);
                responseBody = "{\"message\":\"Third-party loan data saved in Redis\",\"redisKey\":\""
                        + redisKey + "\",\"loan\":" + loanJson + "}";
            }
        } catch (Exception exception) {
            statusCode = 500;
            responseBody = "{\"error\":\"" + HttpExchangeUtils.escapeJson(exception.getMessage()) + "\"}";
        }

        String auditStrategy = auditLoggingService.log(
                loggingStrategyProperties.loan(),
                "third-party-loan-data",
                request,
                new ApiResponse(statusCode, responseBody)
        );
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Audit-Log-Strategy", auditStrategy)
                .body(responseBody);
    }

    private String redisLoanKey(String customerId) {
        return "third-party:loan:" + customerId;
    }

    private String loanResponseFor(String customerId) {
        String escapedCustomerId = HttpExchangeUtils.escapeJson(customerId);
        return switch (customerId) {
            case "C001" -> baseLoanJson(escapedCustomerId, true, "APPROVED", 750000, 10.25, 60);
            case "C002" -> baseLoanJson(escapedCustomerId, true, "APPROVED", 250000, 11.75, 36);
            case "C003" -> baseLoanJson(escapedCustomerId, false, "REJECTED", 0, 0.00, 0);
            case "C004" -> baseLoanJson(escapedCustomerId, true, "PENDING_DOCUMENTS", 500000, 12.10, 48);
            case "C005" -> baseLoanJson(escapedCustomerId, true, "APPROVED", 2500000, 9.85, 120);
            default -> baseLoanJson(escapedCustomerId, true, "PRE_APPROVED", 100000, 13.50, 24);
        };
    }

    private String baseLoanJson(
            String customerId,
            boolean eligible,
            String decision,
            int approvedLimit,
            double interestRate,
            int tenureMonths
    ) {
        return "{"
                + "\"provider\":\"mock-loan-provider\","
                + "\"customerId\":\"" + customerId + "\","
                + "\"eligible\":" + eligible + ","
                + "\"decision\":\"" + decision + "\","
                + "\"approvedLimit\":" + approvedLimit + ","
                + "\"interestRate\":" + String.format("%.2f", interestRate) + ","
                + "\"tenureMonths\":" + tenureMonths + ","
                + "\"currency\":\"INR\""
                + "}";
    }
}
