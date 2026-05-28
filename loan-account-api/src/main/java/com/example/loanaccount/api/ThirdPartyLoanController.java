package com.example.loanaccount.api;

import com.example.loanaccount.model.LoanProviderDataRequest;
import com.example.loanaccount.service.RedisClient;
import com.example.loanaccount.util.HttpExchangeUtils;
import com.example.loanaccount.util.ValidationUtils;
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

    public ThirdPartyLoanController(RedisClient redisClient) {
        this.redisClient = redisClient;
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
    public ResponseEntity<String> upsertLoanData(@RequestBody LoanProviderDataRequest body) {
        if (!ValidationUtils.isValidBankingId(body.customerId())) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Invalid customerId format\"}");
        }

        String redisKey = redisLoanKey(body.customerId());
        String loanJson = body.toLoanJson();
        redisClient.set(redisKey, loanJson);
        String response = "{\"message\":\"Third-party loan data saved in Redis\",\"redisKey\":\""
                + redisKey + "\",\"loan\":" + loanJson + "}";
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(response);
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
