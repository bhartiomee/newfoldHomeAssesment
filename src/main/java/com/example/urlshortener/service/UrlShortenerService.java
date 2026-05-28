package com.example.urlshortener.service;

import com.example.urlshortener.domain.Alias;
import com.example.urlshortener.domain.AliasStatus;
import com.example.urlshortener.domain.CachedUrlMapping;
import com.example.urlshortener.domain.MappingStatus;
import com.example.urlshortener.domain.PlanType;
import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.domain.User;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.CustomAliasNotAllowedException;
import com.example.urlshortener.exception.DuplicateShortKeyException;
import com.example.urlshortener.exception.UserNotFoundException;
import com.example.urlshortener.id.Base62Encoder;
import com.example.urlshortener.id.IdGenerator;
import com.example.urlshortener.ports.AliasRepository;
import com.example.urlshortener.ports.UrlCache;
import com.example.urlshortener.ports.UrlMappingRepository;
import com.example.urlshortener.ports.UserRepository;
import com.example.urlshortener.util.AliasValidator;
import com.example.urlshortener.util.UrlValidator;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UrlShortenerService {
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final UrlMappingRepository urlMappingRepository;
    private final AliasRepository aliasRepository;
    private final UserRepository userRepository;
    private final UrlCache urlCache;
    private final IdGenerator idGenerator;
    private final ShortUrlBuilder shortUrlBuilder;
    private final CacheTtlPolicy cacheTtlPolicy;
    private final Clock clock;
    private final Lock namespaceLock = new ReentrantLock();

    public UrlShortenerService(
            UrlMappingRepository urlMappingRepository,
            AliasRepository aliasRepository,
            UserRepository userRepository,
            UrlCache urlCache,
            IdGenerator idGenerator,
            ShortUrlBuilder shortUrlBuilder,
            CacheTtlPolicy cacheTtlPolicy,
            Clock clock
    ) {
        this.urlMappingRepository = Objects.requireNonNull(urlMappingRepository, "urlMappingRepository");
        this.aliasRepository = Objects.requireNonNull(aliasRepository, "aliasRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.urlCache = Objects.requireNonNull(urlCache, "urlCache");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.shortUrlBuilder = Objects.requireNonNull(shortUrlBuilder, "shortUrlBuilder");
        this.cacheTtlPolicy = Objects.requireNonNull(cacheTtlPolicy, "cacheTtlPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        Objects.requireNonNull(request, "request");
        UrlValidator.requireValidHttpUrl(request.longUrl());
        requireFutureExpiry(request.expiresAt());

        Optional<String> alias = request.customAliasOptional();
        return alias.map(s -> createWithCustomAlias(request, s)).orElseGet(() -> createWithGeneratedKey(request));
    }

    public RedirectResult resolveRedirect(String shortKey) {
        Objects.requireNonNull(shortKey, "shortKey");
        Instant now = clock.instant();

        Optional<CachedUrlMapping> cached = urlCache.get(shortKey);
        if (cached.isPresent()) {
            CachedUrlMapping mapping = cached.get();
            if (mapping.isRedirectableAt(now)) {
                return RedirectResult.Found.temporaryRedirect(mapping.longUrl());
            }
            return inactiveResult(shortKey, mapping.status(), mapping.isExpiredAt(now));
        }

        Optional<UrlMapping> stored = urlMappingRepository.findByShortKey(shortKey);
        if (stored.isEmpty()) {
            return new RedirectResult.NotFound(shortKey);
        }

        UrlMapping mapping = stored.get();
        if (!mapping.isRedirectableAt(now)) {
            urlCache.evict(shortKey);
            return inactiveResult(shortKey, mapping.status(), mapping.isExpiredAt(now));
        }

        urlCache.put(shortKey, CachedUrlMapping.from(mapping), cacheTtlPolicy.ttlFor(mapping.expiresAt()));
        return RedirectResult.Found.temporaryRedirect(mapping.longUrl());
    }

    private ShortUrlResponse createWithGeneratedKey(CreateShortUrlRequest request) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            long id = idGenerator.nextId();
            String shortKey = Base62Encoder.encode(id);
            UrlMapping mapping = newMapping(id, shortKey, request);

            try {
                UrlMapping saved = urlMappingRepository.save(mapping);
                cache(saved);
                return toResponse(saved);
            } catch (DuplicateShortKeyException ignored) {
                // Extremely unlikely with Snowflake IDs; retry keeps the API resilient.
            }
        }
        throw new IllegalStateException("Could not generate a unique short key");
    }

    private ShortUrlResponse createWithCustomAlias(CreateShortUrlRequest request, String aliasKey) {
        AliasValidator.requireValidAlias(aliasKey);
        User user = requirePaidUser(request.userId());
        namespaceLock.lock();
        try {
            if (urlMappingRepository.existsByShortKey(aliasKey)) {
                throw new AliasAlreadyExistsException(aliasKey);
            }

            long urlId = idGenerator.nextId();
            UrlMapping mapping = urlMappingRepository.save(newMapping(urlId, aliasKey, request));
            long aliasId = idGenerator.nextId();
            aliasRepository.save(new Alias(
                    aliasId,
                    user.id(),
                    mapping.id(),
                    aliasKey,
                    AliasStatus.ACTIVE,
                    clock.instant(),
                    request.expiresAt()
            ));
            cache(mapping);
            return toResponse(mapping);
        } finally {
            namespaceLock.unlock();
        }
    }

    private UrlMapping newMapping(long id, String shortKey, CreateShortUrlRequest request) {
        return new UrlMapping(
                id,
                shortKey,
                request.longUrl(),
                request.userId(),
                clock.instant(),
                request.expiresAt(),
                MappingStatus.ACTIVE
        );
    }

    private void cache(UrlMapping mapping) {
        urlCache.put(mapping.shortKey(), CachedUrlMapping.from(mapping), cacheTtlPolicy.ttlFor(mapping.expiresAt()));
    }

    private ShortUrlResponse toResponse(UrlMapping mapping) {
        return new ShortUrlResponse(
                mapping.id(),
                mapping.shortKey(),
                shortUrlBuilder.build(mapping.shortKey()),
                mapping.longUrl(),
                mapping.expiresAt()
        );
    }

    private RedirectResult inactiveResult(String shortKey, MappingStatus status, boolean expired) {
        if (expired || status == MappingStatus.EXPIRED) {
            return new RedirectResult.Expired(shortKey);
        }
        return new RedirectResult.NotActive(shortKey);
    }

    private void requireFutureExpiry(Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
    }

    private User requirePaidUser(Long userId) {
        if (userId == null) {
            throw new UserNotFoundException(0L);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.planType() == PlanType.FREE) {
            throw new CustomAliasNotAllowedException(userId);
        }
        return user;
    }
}
