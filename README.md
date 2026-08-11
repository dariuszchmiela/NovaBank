# NovaBank

Demo project showing a bank transfer integration pattern over Apache Kafka, built as a technical proof of competency.
Focus: reliable, idempotent event delivery — no double-booked transfers.

## Architecture

```
REST client → Producer (validation) → Kafka topic → Consumer (idempotent) → PostgreSQL ledger
                                                            │
                                                            └─ on repeated failure → DLT
```

## Tech stack

- Java 25
- Spring Boot 4.1.0
- Apache Kafka (`spring-boot-starter-kafka`), KRaft mode (no ZooKeeper)
- PostgreSQL + Liquibase (XML changelogs)
- Testcontainers (Kafka + PostgreSQL) for integration tests

## Producer service

- `POST /api/v1/transfers` — accepts a transfer request, validates it (`@Valid`), publishes a `TransferRequestedEvent`
  to Kafka
- **API versioning** via Spring's native request-header versioning (`X-API-Version`, defaults to `1`)
- **Producer reliability**: `acks=all` + `enable.idempotence=true` — no message loss, no duplicate delivery on retry
- **Partitioning**: events are keyed by `sourceAccountId`, guaranteeing ordering per account
- **Error handling**: `@RestControllerAdvice` returns RFC 7807 `ProblemDetail` for validation failures

## Consumer service

- `@KafkaListener` on the transfer-requested topic, manual offset acknowledgment (`ack-mode: manual_immediate` — offset
  only commits after successful processing)
- **Idempotency**: insert-and-catch on a unique `transfer_id` constraint in PostgreSQL (`processed_transfers` table).
  Duplicate deliveries are detected by the database itself, not application logic — no race condition window
- **Dead-letter queue**: `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` — 3 retries with a 1s backoff, then the
  message is published to a dedicated DLT topic and the offset is committed, so a single bad message can't block the
  partition indefinitely

## Tests

- Unit test — mocked `KafkaTemplate`, verifies event construction and publish call
- Integration tests (real Kafka + real PostgreSQL via Testcontainers):
    - Producer: event is actually delivered to the topic
    - Consumer: first delivery is persisted; a duplicate delivery is skipped while subsequent messages keep being
      processed
    - DLQ: a message that keeps failing ends up on the DLT topic

Integration tests require a running Docker daemon.

```bash
mvn test
```

## Running locally

`docker-compose.yml` starts the infrastructure (PostgreSQL + Kafka, KRaft mode) on the same ports the app expects
(`5432`, `9092`):

```bash
docker compose up -d
```

Then run the application from your IDE — no extra configuration needed.

## Coming next

- Outbox pattern (Producer) — atomic persist-and-publish, so a transfer request survives a Kafka outage instead of being
  lost