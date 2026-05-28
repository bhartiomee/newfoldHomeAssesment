package com.example.urlshortener.repository.inmemory;

import com.example.urlshortener.domain.Alias;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.ports.AliasRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAliasRepository implements AliasRepository {
    private final ConcurrentMap<String, Alias> byAliasKey = new ConcurrentHashMap<>();

    @Override
    public Alias save(Alias alias) {
        Alias existing = byAliasKey.putIfAbsent(alias.aliasKey(), alias);
        if (existing != null) {
            throw new AliasAlreadyExistsException(alias.aliasKey());
        }
        return alias;
    }

    @Override
    public Optional<Alias> findByAliasKey(String aliasKey) {
        return Optional.ofNullable(byAliasKey.get(aliasKey));
    }
}
