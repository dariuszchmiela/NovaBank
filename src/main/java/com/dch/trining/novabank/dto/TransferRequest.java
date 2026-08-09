package com.dch.trining.novabank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank
        String sourceAccountId,

        @NotBlank
        String targetAccountId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotBlank
        String currency

) {
}