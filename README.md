# URL Shortener HLD Code Skeleton

This is a dependency-light Java implementation of the URL shortener design.

## Modules

- `domain`: immutable entities and statuses.
- `id`: Snowflake-style ID generation and Base62 short-key encoding.
- `ports`: repository/cache interfaces.
- `repository.inmemory`: thread-safe local adapters for tests.
- `service`: creation and redirect use cases.
- `util`: URL and alias validation helpers.

## Build and Run

```sh
javac -d out $(find src/main/java src/test/java -name "*.java")
java -cp out com.example.urlshortener.UrlShortenerApplication
java -cp out com.example.urlshortener.UrlShortenerServiceTest
```

The in-memory adapters can be swapped with Redis, DynamoDB, Cassandra, or SQL-backed implementations without changing the service layer.
