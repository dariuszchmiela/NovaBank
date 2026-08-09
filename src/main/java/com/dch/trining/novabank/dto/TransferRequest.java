package com.dch.trining.novabank.dto;

import java.math.BigDecimal;

public record TransferRequest(
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency
) {
}