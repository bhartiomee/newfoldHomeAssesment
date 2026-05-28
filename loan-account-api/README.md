# Loan Account API

Runnable Java banking-style API for:

- `GET /loan`
- `GET /account-details`

It includes Redis-backed account reads, Redis caching for third-party loan responses, audit logging with interchangeable strategies, circuit breaker protection, internal endpoint auth, bulkhead pools, Docker, and health/status endpoints.

## Architecture

```text
Client
  |
  v
HttpServer dispatcher-pool
  |
  +--> /loan -> loan-pool
  |      |
  |      +--> validate customerId
  |      +--> Redis cache lookup: loan:customer:{customerId}
  |      +--> retry timeout/connect failures
  |      +--> CircuitBreaker: loan-third-party
  |      +--> third-party loan provider
  |      +--> Redis cache write with TTL
  |      +--> AuditLoggingService
  |
  +--> /account-details -> account-pool
  |      |
  |      +--> validate accountId
  |      +--> Redis account lookup: account:{accountId}
  |      +--> AuditLoggingService
  |
  +--> /logs/db, /health, /circuit-breaker/* -> internal-pool

AuditLoggingService
  |
  +--> LoggingStrategyResolver
        |
        +--> if circuit breaker OPEN: force filesystem logging
        +--> otherwise: configured logging.defaultStrategy
              |
              +--> FileSystemLogStrategy
              +--> DatabaseLogStrategy
```

## Run With Docker

```bash
cd "/Users/omeebharti/Documents/New project/loan-account-api"
docker compose up --build
```

Docker starts:

- `loan-account-api` on `localhost:8080`
- Redis on `localhost:6379`

Internal API key in Docker Compose:

```text
dev-internal-key
```

## Run Locally

Requires Maven and Redis running locally.

```bash
cd "/Users/omeebharti/Documents/New project/loan-account-api"
./run.sh
```

## API Examples

`logTo` query param is ignored even if sent. Logging destination comes from config, unless the circuit breaker is OPEN, in which case audit logs are forced to filesystem.

```bash
curl -H "X-Request-Id: req-loan-1" "http://localhost:8080/loan?customerId=C001"
curl -H "X-Request-Id: req-account-1" "http://localhost:8080/account-details?accountId=A1001"
```

Validation failure:

```bash
curl "http://localhost:8080/loan?customerId=bad-id"
curl "http://localhost:8080/account-details?accountId=bad-id"
```

DB audit logs require `X-Internal-Key` when `internal.api.key` is configured:

```bash
curl -H "X-Internal-Key: dev-internal-key" "http://localhost:8080/logs/db"
```

Filesystem logs:

```bash
tail -n 20 logs/api-requests-2026-05-28.log
```

Health:

```bash
curl "http://localhost:8080/health"
```

Circuit breaker status:

```bash
curl "http://localhost:8080/circuit-breaker/status"
```

Manual circuit breaker reset:

```bash
curl -X POST -H "X-Internal-Key: dev-internal-key" "http://localhost:8080/circuit-breaker/reset"
```

## Configuration

Configuration is read from `application.properties`; environment variables override properties by converting dots to underscores and uppercasing.

Examples:

```text
logging.defaultStrategy=db
internal.api.key=dev-internal-key
loan.http.max.retries=2
loan.cache.ttl.seconds=300
circuitBreaker.loan.failureThreshold=3
redis.host=localhost
```

## Production-Oriented Behaviors

- `X-Request-Id` correlation ID is accepted or generated.
- `customerId` and `accountId` must match `^[A-Z0-9]{1,20}$`.
- Audit query values for `customerId` and `accountId` are masked.
- Audit response `balance` values are masked with a regex before writing logs.
- Third-party loan calls retry only `ConnectException` and `HttpTimeoutException`.
- HTTP 4xx from third-party is never retried.
- Circuit breaker OPEN forces filesystem audit logging for all APIs.
- `/logs/db` and circuit breaker reset are protected by `X-Internal-Key`.
- Separate bounded pools isolate loan, account, and internal traffic.
- Shutdown hook drains server and all worker pools.
