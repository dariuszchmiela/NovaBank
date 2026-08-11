# NovaBank

Demo project showing a bank transfer integration pattern over Apache Kafka, built as a technical proof of competency.
Focus: reliable, idempotent event delivery — no double-booked, no lost transfers, even through a Kafka outage.

## Architecture

```
REST client → Producer → outbox table (same DB transaction) → poller → Kafka topic → Consumer (idempotent) → PostgreSQL ledger
                                                                                              │
                                                                                              └─ on repeated failure → DLT
```

## Tech stack

- Java 25
- Spring Boot 4.1.0
- Apache Kafka (`spring-boot-starter-kafka`), KRaft mode (no ZooKeeper)
- PostgreSQL + Liquibase (XML changelogs)
- Testcontainers (Kafka + PostgreSQL) for integration tests
- REST Assured for HTTP contract tests
- GitHub Actions for CI

## Producer service

- `POST /api/v1/transfers` — accepts a transfer request, validates it (`@Valid`), builds a `TransferRequestedEvent`
- **API versioning** via Spring's native request-header versioning (`X-API-Version`, defaults to `1`)
- **Error handling**: `@RestControllerAdvice` returns RFC 7807 `ProblemDetail` for validation failures
- **Outbox pattern**: instead of publishing to Kafka directly, the event is serialized and saved to an `outbox_events`
  table in the same database transaction as the REST request. The request succeeds as soon as the write is durable — it
  no longer depends on Kafka being reachable at that moment
- **Outbox publisher**: a scheduled poller (`OutboxPublisherScheduler`, configurable interval) reads unpublished rows
  and publishes them with `acks=all` + `enable.idempotence=true`, keyed by `sourceAccountId` for per-account ordering. A
  row is only marked published after Kafka confirms the write — a failed attempt is retried on the next poll, nothing is
  lost

## Consumer service

- `@KafkaListener` on the transfer-requested topic, manual offset acknowledgment (`ack-mode: manual_immediate` — offset
  only commits after successful processing)
- **Idempotency**: insert-and-catch on a unique `transfer_id` constraint in PostgreSQL (`processed_transfers` table).
  Duplicate deliveries — from Kafka retries or from the outbox poller re-sending after a late confirmation — are
  detected by the database itself, not application logic — no race condition window
- **Dead-letter queue**: `ErrorHandlingDeserializer` + `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` — a
  message that fails to deserialize *or* fails processing gets 3 retries with a 1s backoff, then is published to a
  dedicated DLT topic and the offset is committed, so one bad message can't block the partition indefinitely

## Reliability, end to end

At-least-once delivery at every hop (outbox → Kafka → consumer) combined with idempotent processing at the point that
matters (the ledger insert) gives an effectively-exactly-once outcome without needing a distributed transaction.

## Tests

- Unit test — mocked repository, verifies the event is correctly built and saved to the outbox
- Integration tests (real Kafka + real PostgreSQL via Testcontainers):
    - Producer → outbox → poller: event is actually delivered to the topic
    - Consumer: first delivery is persisted; a duplicate delivery is skipped while subsequent messages keep being
      processed
    - DLQ: a message that keeps failing ends up on the DLT topic
- Contract test (REST Assured) — real HTTP calls against a running embedded server (`RANDOM_PORT`), verifying the actual
  request/response contract of `POST /api/transfers`: success shape and status code, and the `ProblemDetail`
  shape returned on validation failure

Integration tests require a running Docker daemon.

```bash
mvn test
```

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs the full test suite — including the Testcontainers-based integration
tests — on every push and pull request to `master`.

## Running locally

`docker-compose.yml` starts the infrastructure (PostgreSQL + Kafka, KRaft mode) on the same ports the app expects
(`5432`, `9092`):

```bash
docker compose up -d
```

Then run the application from your IDE — no extra configuration needed.