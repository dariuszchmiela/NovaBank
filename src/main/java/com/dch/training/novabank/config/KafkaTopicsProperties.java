package com.dch.training.novabank.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "novabank.kafka.topics")
@Validated
public record KafkaTopicsProperties(
        @NotBlank
        String transferRequested,

        @NotBlank
        String transferRequestedDlt
) {
}