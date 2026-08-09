# NovaBank

Demo project showing a bank transfer integration pattern over Apache Kafka, built as a technical proof of competency.
Focus: reliable, idempotent event delivery — no double-booked transfers.

## Architecture

```
REST client → Producer (validation) → Kafka topic → [Consumer — in progress]
```

## Tech stack

- Java 25
- Spring Boot 4.1.0
- Apache Kafka (`spring-boot-starter-kafka`)
- Testcontainers (Kafka) for integration tests

## Implemented so far: Producer service

- `POST /api/v1/transfers` — accepts a transfer request, validates it (`@Valid`), publishes a `TransferRequestedEvent`
  to Kafka
- **API versioning** via Spring's native request-header versioning (`X-API-Version`, defaults to `1`) instead of
  encoding the version in the URL
- **Producer reliability**: `acks=all` + `enable.idempotence=true` — no message loss, no duplicate delivery on retry
- **Partitioning**: events are keyed by `sourceAccountId`, guaranteeing ordering per account
- **Error handling**: `@RestControllerAdvice` returns RFC 7807 `ProblemDetail` for validation failures
- **Tests**:
    - Unit test — mocked `KafkaTemplate`, verifies event construction and publish call
    - Integration test — real Kafka broker via Testcontainers, verifies the event is actually delivered to the topic

## Running tests

Integration tests require a running Docker daemon (Testcontainers spins up a real Kafka broker).

```bash
mvn test
```

## Coming next

- Consumer service — idempotent processing with a PostgreSQL ledger (`processed_transfers`)
- Dead-letter queue for failed messages
- Outbox pattern (Producer) for atomic persist-and-publish
- Docker Compose for local end-to-end run