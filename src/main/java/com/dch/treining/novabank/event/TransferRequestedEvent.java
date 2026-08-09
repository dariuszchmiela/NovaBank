package com.dch.treining.novabank.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TransferRequestedEvent(
        String transferId,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency,
        Instant requestedAt
) {

    public TransferRequestedEvent {
        Objects.requireNonNull(transferId, "transferId must not be null");
        Objects.requireNonNull(sourceAccountId, "sourceAccountId must not be null");
        Objects.requireNonNull(targetAccountId, "targetAccountId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }
}