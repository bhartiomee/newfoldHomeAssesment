package com.example.urlshortener.ports;

import com.example.urlshortener.domain.Alias;

import java.util.Optional;

public interface AliasRepository {
    Alias save(Alias alias);

    Optional<Alias> findByAliasKey(String aliasKey);
}
