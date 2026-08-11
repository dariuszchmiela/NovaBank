package com.dch.training.novabank.service;

import com.dch.training.novabank.config.KafkaTopicsProperties;
import com.dch.training.novabank.dto.TransferRequest;
import com.dch.training.novabank.entity.OutboxEvent;
import com.dch.training.novabank.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferProducerServiceTest {

    private static final String SOURCE_ACCOUNT_ID = "ACC-SOURCE-1";
    private static final String TARGET_ACCOUNT_ID = "ACC-TARGET-1";
    private static final BigDecimal AMOUNT = new BigDecimal("150.00");
    private static final String CURRENCY = "PLN";
    private static final String TRANSFER_REQUESTED_TOPIC = "bank.transfers.requested";
    private static final String TRANSFER_REQUESTED_DLT_TOPIC = "bank.transfers.requested.dlt";

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void publishTransferRequested_shouldSaveEventToOutbox() {
        // given
        KafkaTopicsProperties kafkaTopicsProperties =
                new KafkaTopicsProperties(TRANSFER_REQUESTED_TOPIC, TRANSFER_REQUESTED_DLT_TOPIC);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        TransferProducerService transferProducerService =
                new TransferProducerService(outboxEventRepository, kafkaTopicsProperties, objectMapper);
        TransferRequest request = new TransferRequest(SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, AMOUNT, CURRENCY);

        // when
        String transferId = transferProducerService.publishTransferRequested(request);

        // then
        ArgumentCaptor<OutboxEvent> outboxEventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxEventCaptor.capture());

        OutboxEvent savedOutboxEvent = outboxEventCaptor.getValue();
        assertThat(savedOutboxEvent.getTopic()).isEqualTo(TRANSFER_REQUESTED_TOPIC);
        assertThat(savedOutboxEvent.getMessageKey()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(savedOutboxEvent.getPayload()).contains(transferId);
        assertThat(savedOutboxEvent.getPayload()).contains(TARGET_ACCOUNT_ID);
        assertThat(savedOutboxEvent.getPublishedAt()).isNull();
    }
}