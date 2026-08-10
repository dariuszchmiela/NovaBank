package com.dch.training.novabank.service;

import com.dch.training.novabank.config.KafkaTopicsProperties;
import com.dch.training.novabank.dto.TransferRequest;
import com.dch.training.novabank.event.TransferRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void publishTransferRequested() {
        // given
        KafkaTopicsProperties kafkaTopicsProperties =
                new KafkaTopicsProperties(TRANSFER_REQUESTED_TOPIC, TRANSFER_REQUESTED_DLT_TOPIC);
        TransferProducerService transferProducerService = new TransferProducerService(kafkaTemplate, kafkaTopicsProperties);
        TransferRequest request = new TransferRequest(SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, AMOUNT, CURRENCY);
        // when
        String transferId = transferProducerService.publishTransferRequested(request);
        ArgumentCaptor<TransferRequestedEvent> eventCaptor = ArgumentCaptor.forClass(TransferRequestedEvent.class);
        // then
        verify(kafkaTemplate).send(eq(TRANSFER_REQUESTED_TOPIC), eq(SOURCE_ACCOUNT_ID), eventCaptor.capture());
        TransferRequestedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.transferId()).isEqualTo(transferId);
        assertThat(publishedEvent.sourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(publishedEvent.targetAccountId()).isEqualTo(TARGET_ACCOUNT_ID);
        assertThat(publishedEvent.amount()).isEqualTo(AMOUNT);
        assertThat(publishedEvent.currency()).isEqualTo(CURRENCY);
        assertThat(publishedEvent.requestedAt()).isNotNull();
    }
}