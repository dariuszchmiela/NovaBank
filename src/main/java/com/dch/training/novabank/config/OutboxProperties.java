package com.dch.training.novabank.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "novabank.outbox")
@Validated
public record OutboxProperties(

        @NotNull
        @Positive
        Long pollIntervalMs,

        @NotNull
        @Positive
        Long publishTimeoutSeconds

) {
}