package com.example.urlshortener.ports;

import com.example.urlshortener.domain.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(long userId);
}
