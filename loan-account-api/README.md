# Loan Account API

Spring Boot banking-style API for:

- `GET /loan`
- `GET /account-details`

The service demonstrates a production-oriented implementation of interchangeable audit logging, Redis reads/cache, Resilience4j circuit breaker protection, API-key protected internal endpoints, Dockerized runtime, and end-to-end testability.

## Code Architecture

```text
Client
  |
  v
Spring Boot + Embedded Tomcat
  |
  +-- LoanController (/loan)
  |     |
  |     +-- validates customerId
  |     +-- dispatches work to loan-pool
  |     +-- LoanService
  |           |
  |           +-- Redis cache lookup: loan:customer:{customerId}
  |           +-- retry timeout/connect failures with exponential backoff
  |           +-- Resilience4j CircuitBreaker: loan-third-party
  |           +-- third-party loan client call
  |           +-- Redis cache write with TTL
  |           +-- AuditLoggingService
  |
  +-- AccountDetailsController (/account-details)
  |     |
  |     +-- validates accountId
  |     +-- dispatches work to account-pool
  |     +-- AccountDetailsService
  |           |
  |           +-- Redis lookup: account:{accountId}
  |           +-- AuditLoggingService
  |
  +-- InternalController
        |
        +-- /health
        +-- /logs/db
        +-- /logs/db/test-entry
        +-- /circuit-breaker/status
        +-- /circuit-breaker/open
        +-- /circuit-breaker/reset

AuditLoggingService
  |
  +-- LoggingStrategyResolver
        |
        +-- if circuit breaker OPEN: force filesystem logging
        +-- otherwise: per-API logging.strategy.* config
              |
              +-- DatabaseLogStrategy
              +-- FileSystemLogStrategy
```

Important packages:

```text
api/          Spring controllers
config/       typed Spring configuration and beans
logging/      audit log model, strategies, resolver, structured logger
resilience/   Resilience4j CircuitBreaker wrapper
security/     internal API-key auth
service/      loan, account, Redis, seeding logic
util/         validation, PII masking, request snapshot helpers
```

## Run In Docker

Prerequisites:

- Docker runtime running
- Docker Compose available

From the project directory:

```bash
cd "/Users/omeebharti/Documents/New project/loan-account-api"
docker compose down
docker compose up -d --build
```

Check containers:

```bash
docker compose ps
```

Expected services:

```text
loan-account-api
redis
```

The app runs at:

```text
http://localhost:8080
```

Docker Compose configures the internal API key as:

```text
dev-internal-key
```

## End-To-End Testing In Docker

Run these commands after `docker compose up -d --build`.

### 1. Health Checks

```bash
curl "http://127.0.0.1:8080/health"
curl "http://127.0.0.1:8080/actuator/health"
```

Expected:

```json
{"status":"UP"}
```

The custom `/health` endpoint also shows circuit breaker state, DB audit log size, and uptime.

### 2. Loan API

```bash
curl -H "X-Request-Id: e2e-loan-1" \
  "http://127.0.0.1:8080/loan?customerId=C001"
```

Expected response includes:

```json
{
  "provider": "mock-loan-provider",
  "customerId": "C001",
  "decision": "APPROVED"
}
```

### 3. Account Details API

```bash
curl -H "X-Request-Id: e2e-account-1" \
  "http://127.0.0.1:8080/account-details?accountId=A1001"
```

Expected response includes:

```json
{
  "accountId": "A1001",
  "holderName": "Omee Bharti",
  "status": "ACTIVE"
}
```

### 4. Add Account Data To Redis

Use this to add more account data, then fetch it through `GET /account-details`.

```bash
curl -i -X POST "http://127.0.0.1:8080/account-details" \
  -H "X-Request-Id: write-account-A2001" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "A2001",
    "holderName": "Test User",
    "balance": 45678.90,
    "currency": "INR",
    "status": "ACTIVE"
  }'
```

The response header tells you where this request was audited:

```text
X-Audit-Log-Strategy: db
```

Fetch the new account:

```bash
curl -H "X-Request-Id: e2e-account-new" \
  "http://127.0.0.1:8080/account-details?accountId=A2001"
```

### 5. Add Mock Loan Provider Data To Redis

Use this to add more third-party loan-provider data. The `/loan` API calls the mock provider, and the mock provider first checks Redis key `third-party:loan:{customerId}`.

```bash
curl -i -X POST "http://127.0.0.1:8080/third-party/loan-data" \
  -H "X-Request-Id: write-loan-C9001" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "C9001",
    "eligible": true,
    "decision": "APPROVED",
    "approvedLimit": 990000,
    "interestRate": 8.75,
    "tenureMonths": 84,
    "currency": "INR"
  }'
```

The response header tells you where this request was audited:

```text
X-Audit-Log-Strategy: file
```

Fetch the newly added mock provider data through the real loan API:

```bash
curl -H "X-Request-Id: e2e-loan-new" \
  "http://127.0.0.1:8080/loan?customerId=C9001"
```

### 6. Input Validation

```bash
curl -i "http://127.0.0.1:8080/loan?customerId=bad-id"
curl -i "http://127.0.0.1:8080/account-details?accountId=bad-id"
```

Expected status:

```text
400
```

Expected body:

```json
{"error":"Invalid customerId format"}
```

or:

```json
{"error":"Invalid accountId format"}
```

### 7. DB Audit Logs

DB logs are protected by `X-Internal-Key`.

Without key:

```bash
curl -i "http://127.0.0.1:8080/logs/db"
```

Expected:

```text
401 Unauthorized
```

With key:

```bash
curl -H "X-Internal-Key: dev-internal-key" \
  "http://127.0.0.1:8080/logs/db"
```

Verify:

- request IDs are present
- `customerId` is masked like `C***1`
- `accountId` is masked like `A***1`
- `balance` is masked as `"[MASKED]"`
- loan logs include `retryCount`
- loan logs include `cacheHit`

You can also write a synthetic audit entry directly into the in-memory audit DB for testing:

```bash
curl -X POST \
  -H "X-Internal-Key: dev-internal-key" \
  "http://127.0.0.1:8080/logs/db/test-entry?customerId=C7777"
```

Then fetch DB logs again:

```bash
curl -H "X-Internal-Key: dev-internal-key" \
  "http://127.0.0.1:8080/logs/db"
```

To confirm a write request was logged in DB, search the response for the request ID you sent:

```text
"requestId":"write-account-A2001"
"loggingStrategy":"db"
```

### 8. Filesystem Audit Logs

Filesystem logs are mounted from the container to the local `logs/` directory.

```bash
tail -n 20 logs/api-requests-2026-05-28.log
```

If the date changed, list the log files:

```bash
ls logs
tail -n 20 logs/api-requests-YYYY-MM-DD.log
```

### 9. Database Used

This project uses two storage mechanisms:

- **Redis**: real Redis container from Docker Compose. It stores account data and loan-response cache entries.
- **Audit DB**: bounded in-memory audit log database implemented by `InMemoryLogDatabase`. It is intentionally lightweight for this assignment and resets when the app restarts. The API exposes it through protected endpoint `GET /logs/db`.

There is no external SQL database in this implementation. For a production system, the `DatabaseLogStrategy` can be replaced with PostgreSQL/MySQL using the same logging strategy interface.

### 10. Circuit Breaker Status

```bash
curl "http://127.0.0.1:8080/circuit-breaker/status"
```

Expected:

```json
{
  "circuitBreaker": "loan-third-party",
  "state": "CLOSED",
  "failedCalls": 0
}
```

### 11. Circuit Breaker Open/Close Test

Use this flow to prove that when the circuit breaker is `OPEN`, audit logs are forced to filesystem.

Open the circuit breaker manually:

```bash
curl -X POST \
  -H "X-Internal-Key: dev-internal-key" \
  "http://127.0.0.1:8080/circuit-breaker/open"
```

Confirm state is `OPEN`:

```bash
curl "http://127.0.0.1:8080/circuit-breaker/status"
curl "http://127.0.0.1:8080/health"
```

Call account API while the breaker is open:

```bash
curl -H "X-Request-Id: cb-open-account-1" \
  "http://127.0.0.1:8080/account-details?accountId=A1001"
```

Verify the log went to filesystem:

```bash
tail -n 20 logs/api-requests-2026-05-28.log
```

Look for:

```text
"requestId":"cb-open-account-1"
"loggingStrategy":"file"
```

Every audited API response also includes `X-Audit-Log-Strategy`, so `curl -i` shows the actual destination immediately.

Reset the circuit breaker back to `CLOSED`:

```bash
curl -X POST \
  -H "X-Internal-Key: dev-internal-key" \
  "http://127.0.0.1:8080/circuit-breaker/reset"
```

Expected:

```json
{
  "circuitBreaker": "loan-third-party",
  "state": "CLOSED",
  "action": "reset"
}
```

Call account API again:

```bash
curl -H "X-Request-Id: cb-closed-account-1" \
  "http://127.0.0.1:8080/account-details?accountId=A1001"
```

Verify that normal DB logging resumed:

```bash
curl -H "X-Internal-Key: dev-internal-key" \
  "http://127.0.0.1:8080/logs/db"
```

Look for:

```text
"requestId":"cb-closed-account-1"
"loggingStrategy":"db"
```

### 12. Verify `logTo` Is Ignored

The API accepts the param but does not use it.

```bash
curl "http://127.0.0.1:8080/loan?customerId=C002&logTo=file"
curl -H "X-Internal-Key: dev-internal-key" "http://127.0.0.1:8080/logs/db"
```

The logging destination still follows config unless the circuit breaker is `OPEN`.

## Load Testing

Install ApacheBench if needed:

```bash
brew install httpd
```

Account API load test:

```bash
/opt/homebrew/opt/httpd/bin/ab -n 1000 -c 50 \
  "http://127.0.0.1:8080/account-details?accountId=A1001"
```

Loan API load test:

```bash
/opt/homebrew/opt/httpd/bin/ab -n 500 -c 25 \
  "http://127.0.0.1:8080/loan?customerId=C001"
```

After load testing, verify health and logs:

```bash
curl "http://127.0.0.1:8080/health"
curl -H "X-Internal-Key: dev-internal-key" "http://127.0.0.1:8080/logs/db"
tail -n 20 logs/api-requests-2026-05-28.log
```

## Configuration

Configuration is read from `application.properties`. Docker Compose sets environment variables for the container.

Important properties:

```text
logging.strategy.loan=file
logging.strategy.account-details=db
app.logging.file-directory=logs
app.logging.db.max-entries=10000
app.internal.api-key=dev-internal-key
app.loan.http.max-retries=2
app.loan.cache-ttl-seconds=300
app.circuit-breaker.loan.failure-threshold=3
app.circuit-breaker.loan.open-duration-millis=30000
app.redis.host=localhost
app.redis.port=6379
```

Docker examples:

```text
APP_REDIS_HOST=redis
APP_INTERNAL_API_KEY=dev-internal-key
APP_LOGGING_FILE_DIRECTORY=/app/logs
LOGGING_STRATEGY_LOAN=file
LOGGING_STRATEGY_ACCOUNT_DETAILS=db
```

`logging.strategy.loan` controls `GET /loan` and `POST /third-party/loan-data` audit logs.
`logging.strategy.account-details` controls `GET /account-details` and `POST /account-details` audit logs.
When the Resilience4j circuit breaker is `OPEN`, the resolver overrides both settings and writes every API audit log to filesystem.

## Non-Functional Requirements Implemented

- **Spring Boot foundation**: chosen for standard production structure, dependency injection, embedded server management, configuration binding, and Actuator support.
- **Request correlation**: `X-Request-Id` is accepted or generated so request/response audit logs can be traced across DB and filesystem.
- **Input validation**: `customerId` and `accountId` must match `^[A-Z0-9]{1,20}$` to prevent malformed downstream calls and noisy logs.
- **PII masking**: audit logs mask `customerId`, `accountId`, and `balance` to reduce sensitive data exposure.
- **Configurable logging strategy**: logging can switch between DB and filesystem through configuration without changing controller/service code.
- **Circuit breaker override**: when the Resilience4j circuit breaker is `OPEN`, all APIs force filesystem audit logging to avoid depending on DB logging during third-party instability.
- **Resilience4j circuit breaker**: protects the third-party loan call from repeated failures and exposes state through internal endpoints.
- **Retry with exponential backoff**: retries only transient network failures before the circuit breaker records the final failure.
- **Redis caching**: loan third-party responses are cached with TTL to reduce latency and dependency pressure.
- **Redis-backed account reads**: account details come from Redis to match the required account API flow.
- **Bulkhead pools**: loan, account, and internal work use separate bounded executor pools to reduce cross-endpoint interference.
- **Internal endpoint protection**: `/logs/db` and manual circuit breaker reset require `X-Internal-Key`.
- **Bounded audit DB log storage**: in-memory DB logs use a bounded ring buffer to avoid unbounded memory growth.
- **Thread-safe filesystem logging**: file audit writes are serialized to prevent interleaved log lines.
- **Health and observability**: `/health`, `/actuator/health`, structured logs, and circuit breaker status endpoints support operations and debugging.
- **Dockerized runtime**: Docker Compose starts both the API and Redis for repeatable local and reviewer testing.

## Stop The Application

```bash
docker compose down
```

Use this if port `8080` is already occupied:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

Stop the listed process only if it is an old local run of this same app.
