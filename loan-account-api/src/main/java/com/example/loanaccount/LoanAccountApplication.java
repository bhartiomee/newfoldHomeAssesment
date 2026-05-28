package com.example.loanaccount;

import com.example.loanaccount.config.AppProperties;
import com.example.loanaccount.config.LoggingStrategyProperties;
import com.example.loanaccount.logging.StructuredLogger;
import com.example.loanaccount.service.RedisClient;
import com.example.loanaccount.service.RedisSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, LoggingStrategyProperties.class})
public class LoanAccountApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanAccountApplication.class, args);
    }

    @Bean
    CommandLineRunner seedRedis(RedisClient redisClient, AppProperties properties) {
        return args -> {
            RedisSeeder.seedAccounts(redisClient);
            StructuredLogger.info("application_started", Map.of(
                    "port", Integer.toString(properties.server().port()),
                    "redisHost", properties.redis().host(),
                    "defaultLoggingStrategy", properties.logging().defaultStrategy()
            ));
        };
    }
}
