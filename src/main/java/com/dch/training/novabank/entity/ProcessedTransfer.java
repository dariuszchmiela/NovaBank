package com.dch.training.novabank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_transfers")
public class ProcessedTransfer {

    @Id
    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "source_account_id", nullable = false)
    private String sourceAccountId;

    @Column(name = "target_account_id", nullable = false)
    private String targetAccountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedTransfer() {
        // required by JPA
    }

    public ProcessedTransfer(UUID transferId, String sourceAccountId, String targetAccountId,
                             BigDecimal amount, String currency, Instant processedAt) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.processedAt = processedAt;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public String getTargetAccountId() {
        return targetAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}