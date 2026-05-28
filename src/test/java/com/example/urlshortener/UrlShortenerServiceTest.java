package com.example.urlshortener;

import com.example.urlshortener.domain.PlanType;
import com.example.urlshortener.domain.User;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.CustomAliasNotAllowedException;
import com.example.urlshortener.exception.InvalidUrlException;
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
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class UrlShortenerServiceTest {
    public static void main(String[] args) throws Exception {
        shouldCreateAndResolveGeneratedShortUrl();
        shouldCreateAndResolveCustomAlias();
        shouldRejectDuplicateAlias();
        shouldRejectCustomAliasForFreeUser();
        shouldRejectInvalidUrl();
        shouldReturnExpiredForExpiredMappings();
        shouldGenerateUniqueIdsConcurrently();
        System.out.println("All tests passed");
    }

    private static void shouldCreateAndResolveGeneratedShortUrl() {
        ManualClock clock = new ManualClock(Instant.parse("2026-05-26T00:00:00Z"));
        UrlShortenerService service = newService(clock);

        ShortUrlResponse response = service.createShortUrl(new CreateShortUrlRequest(
                "https://example.com/a/b",
                1L,
                null,
                clock.instant().plus(Duration.ofDays(1))
        ));

        assertTrue(response.shortUrl().contains(response.shortKey()), "short URL should include key");
        RedirectResult result = service.resolveRedirect(response.shortKey());
        assertTrue(result instanceof RedirectResult.Found, "redirect should be found");
        assertEquals("https://example.com/a/b", ((RedirectResult.Found) result).longUrl());
        assertEquals(302, result.httpStatus().code());
    }

    private static void shouldCreateAndResolveCustomAlias() {
        ManualClock clock = new ManualClock(Instant.parse("2026-05-26T00:00:00Z"));
        InMemoryAliasRepository aliasRepository = new InMemoryAliasRepository();
        UrlShortenerService service = newService(clock, aliasRepository);

        ShortUrlResponse response = service.createShortUrl(new CreateShortUrlRequest(
                "https://example.com/sale",
                1L,
                "summer-sale",
                null
        ));

        assertEquals("summer-sale", response.shortKey());
        assertTrue(service.resolveRedirect("summer-sale") instanceof RedirectResult.Found, "alias should redirect");
        assertTrue(aliasRepository.findByAliasKey("summer-sale").isPresent(), "alias metadata should be stored");
    }

    private static void shouldRejectDuplicateAlias() {
        ManualClock clock = new ManualClock(Instant.parse("2026-05-26T00:00:00Z"));
        UrlShortenerService service = newService(clock);
        service.createShortUrl(new CreateShortUrlRequest("https://example.com/one", 1L, "shared-key", null));

        assertThrows(AliasAlreadyExistsException.class, () ->
                service.createShortUrl(new CreateShortUrlRequest("https://example.com/two", 2L, "shared-key", null)));
    }

    private static void shouldRejectCustomAliasForFreeUser() {
        ManualClock clock = new ManualClock(Instant.parse("2026-05-26T00:00:00Z"));
        UrlShortenerService service = newService(clock);

        assertThrows(CustomAliasNotAllowedException.class, () ->
                service.createShortUrl(new CreateShortUrlRequest("https://example.com/free", 3L, "free-alias", null)));
    }

    private static void shouldRejectInvalidUrl() {
        UrlShortenerService service = newService(new ManualClock(Instant.parse("2026-05-26T00:00:00Z")));

        assertThrows(InvalidUrlException.class, () ->
                service.createShortUrl(new CreateShortUrlRequest("ftp://example.com/file", 1L, null, null)));
    }

    private static void shouldReturnExpiredForExpiredMappings() {
        ManualClock clock = new ManualClock(Instant.parse("2026-05-26T00:00:00Z"));
        UrlShortenerService service = newService(clock);
        ShortUrlResponse response = service.createShortUrl(new CreateShortUrlRequest(
                "https://example.com/temp",
                1L,
                null,
                clock.instant().plus(Duration.ofMinutes(5))
        ));

        clock.advance(Duration.ofMinutes(6));
        assertTrue(service.resolveRedirect(response.shortKey()) instanceof RedirectResult.Expired, "mapping should expire");
    }

    private static void shouldGenerateUniqueIdsConcurrently() throws Exception {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(SnowflakeConfig.singleDatacenter(2), Clock.systemUTC());
        int workers = 8;
        int idsPerWorker = 1_000;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        Set<Long> ids = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < workers; i++) {
            executor.submit(() -> {
                await(start);
                for (int j = 0; j < idsPerWorker; j++) {
                    ids.add(generator.nextId());
                }
            });
        }

        start.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "executor should finish");
        assertEquals(workers * idsPerWorker, ids.size());
    }

    private static UrlShortenerService newService(Clock clock) {
        return newService(clock, new InMemoryAliasRepository());
    }

    private static UrlShortenerService newService(Clock clock, InMemoryAliasRepository aliasRepository) {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User(1L, "pro@example.com", "Pro User", clock.instant(), PlanType.PRO));
        userRepository.save(new User(2L, "enterprise@example.com", "Enterprise User", clock.instant(), PlanType.ENTERPRISE));
        userRepository.save(new User(3L, "free@example.com", "Free User", clock.instant(), PlanType.FREE));

        return new UrlShortenerService(
                new InMemoryUrlMappingRepository(),
                aliasRepository,
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

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> type, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (type.isInstance(throwable)) {
                return;
            }
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + throwable);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " but nothing was thrown");
    }

    private static final class ManualClock extends Clock {
        private Instant current;

        private ManualClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
