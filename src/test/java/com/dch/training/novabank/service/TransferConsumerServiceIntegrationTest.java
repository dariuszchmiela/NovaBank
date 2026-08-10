package com.dch.training.novabank.service;

import com.dch.training.novabank.AbstractIntegrationTest;
import com.dch.training.novabank.config.KafkaTopicsProperties;
import com.dch.training.novabank.event.TransferRequestedEvent;
import com.dch.training.novabank.repository.ProcessedTransferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class TransferConsumerServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String SOURCE_ACCOUNT_ID = "ACC-SOURCE-CONSUMER-IT";
    private static final String TARGET_ACCOUNT_ID = "ACC-TARGET-CONSUMER-IT";
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");
    private static final String CURRENCY = "PLN";
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaTopicsProperties kafkaTopicsProperties;

    @Autowired
    private ProcessedTransferRepository processedTransferRepository;

    @Test
    void shouldPersistTransferOnFirstDelivery() {
        UUID transferId = UUID.randomUUID();

        publishEvent(buildEvent(transferId));

        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
                assertThat(processedTransferRepository.findById(transferId)).isPresent());
    }

    @Test
    void shouldSkipDuplicateAndKeepProcessingSubsequentMessages() {
        UUID duplicatedTransferId = UUID.randomUUID();
        UUID nextTransferId = UUID.randomUUID();

        publishEvent(buildEvent(duplicatedTransferId));
        publishEvent(buildEvent(duplicatedTransferId));
        publishEvent(buildEvent(nextTransferId));

        await().atMost(AWAIT_TIMEOUT).untilAsserted(() -> {
            assertThat(processedTransferRepository.findById(duplicatedTransferId)).isPresent();
            assertThat(processedTransferRepository.findById(nextTransferId)).isPresent();
        });
    }

    private void publishEvent(TransferRequestedEvent event) {
        kafkaTemplate.send(kafkaTopicsProperties.transferRequested(), SOURCE_ACCOUNT_ID, event);
    }

    private TransferRequestedEvent buildEvent(UUID transferId) {
        return new TransferRequestedEvent(
                transferId.toString(),
                SOURCE_ACCOUNT_ID,
                TARGET_ACCOUNT_ID,
                AMOUNT,
                CURRENCY,
                Instant.now()
        );
    }
}