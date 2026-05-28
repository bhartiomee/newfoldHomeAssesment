package com.example.loanaccount.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logging.strategy")
public record LoggingStrategyProperties(
        String loan,
        String accountDetails
) {
    public LoggingStrategyProperties {
        if (loan == null || loan.isBlank()) loan = "file";
        if (accountDetails == null || accountDetails.isBlank()) accountDetails = "db";
    }
}
