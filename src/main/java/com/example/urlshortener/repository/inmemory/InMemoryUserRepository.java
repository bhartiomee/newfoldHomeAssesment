package com.example.urlshortener.repository.inmemory;

import com.example.urlshortener.domain.User;
import com.example.urlshortener.ports.UserRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryUserRepository implements UserRepository {
    private final ConcurrentMap<Long, User> byId = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        byId.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findById(long userId) {
        return Optional.ofNullable(byId.get(userId));
    }
}
