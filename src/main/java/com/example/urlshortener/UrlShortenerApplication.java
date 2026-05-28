package com.example.urlshortener;

import com.example.urlshortener.domain.PlanType;
import com.example.urlshortener.domain.User;
import com.example.urlshortener.id.SnowflakeConfig;
import com.example.urlshortener.id.SnowflakeIdGenerator;
import com.example.urlshortener.repository.inmemory.InMemoryAliasRepository;
import com.example.urlshortener.repository.inmemory.InMemoryUrlCache;
import com.example.urlshortener.repository.inmemory.InMemoryUrlMappingRepository;
import com.example.urlshortener.repository.inmemory.InMemoryUserRepository;
import com.example.urlshortener.service.CacheTtlPolicy;
import com.example.urlshortener.service.CreateShortUrlRequest;
import com.example.urlshortener.service.RedirectResult;
import com.example.urlshortener.service.ShortUrlBuilder;
import com.example.urlshortener.service.ShortUrlResponse;
import com.example.urlshortener.service.UrlShortenerService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class UrlShortenerApplication {
    private static final long PAID_USER_ID = 101L;

    private UrlShortenerApplication() {
    }

    public static void main(String[] args) {
        UrlShortenerService service = createService();

        ShortUrlResponse response = service.createShortUrl(new CreateShortUrlRequest(
                "https://example.com/articles/designing-a-url-shortener",
                PAID_USER_ID,
                null,
                null
        ));

        ShortUrlResponse aliasResponse = service.createShortUrl(new CreateShortUrlRequest(
                "https://example.com/summer-sale",
                PAID_USER_ID,
                "summer-sale",
                Instant.now().plus(Duration.ofDays(30))
        ));

        printCreatedUrl("Generated short URL", response);
        printCreatedUrl("Custom alias URL", aliasResponse);

        RedirectResult redirectResult = service.resolveRedirect(response.shortKey());
        if (redirectResult instanceof RedirectResult.Found found) {
            System.out.println("Redirect status: " + found.httpStatus().code());
            System.out.println("Redirect location: " + found.longUrl());
        } else {
            System.out.println("Redirect status: " + redirectResult.httpStatus().code());
        }
    }

    private static UrlShortenerService createService() {
        Clock clock = Clock.systemUTC();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User(
                PAID_USER_ID,
                "paid-user@example.com",
                "Paid User",
                clock.instant(),
                PlanType.PRO
        ));

        return new UrlShortenerService(
                new InMemoryUrlMappingRepository(),
                new InMemoryAliasRepository(),
                userRepository,
                new InMemoryUrlCache(clock),
                new SnowflakeIdGenerator(SnowflakeConfig.singleDatacenter(1), clock),
                new ShortUrlBuilder("https://sho.rt"),
                CacheTtlPolicy.builder(clock)
                        .defaultCacheTtl(Duration.ofHours(6))
                        .maxCacheTtl(Duration.ofDays(1))
                        .build(),
                clock
        );
    }

    private static void printCreatedUrl(String label, ShortUrlResponse response) {
        System.out.println(label + ": " + response.shortUrl());
        System.out.println("Short key: " + response.shortKey());
        System.out.println("Long URL: " + response.longUrl());
    }
}
