package com.example.urlshortener.domain;

import java.time.Instant;
import java.util.Objects;

public record User(
        long id,
        String email,
        String name,
        Instant createdAt,
        PlanType planType
) {
    public User {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(planType, "planType");
    }
}
