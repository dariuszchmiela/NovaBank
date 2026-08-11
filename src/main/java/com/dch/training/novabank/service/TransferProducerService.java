package com.dch.training.novabank.service;

import com.dch.training.novabank.config.KafkaTopicsProperties;
import com.dch.training.novabank.dto.TransferRequest;
import com.dch.training.novabank.entity.OutboxEvent;
import com.dch.training.novabank.event.TransferRequestedEvent;
import com.dch.training.novabank.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransferProducerService {

    private static final Logger log = LoggerFactory.getLogger(TransferProducerService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTopicsProperties kafkaTopicsProperties;
    private final ObjectMapper outboxObjectMapper;

    public TransferProducerService(OutboxEventRepository outboxEventRepository,
                                   KafkaTopicsProperties kafkaTopicsProperties,
                                   @Qualifier("outboxObjectMapper") ObjectMapper outboxObjectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTopicsProperties = kafkaTopicsProperties;
        this.outboxObjectMapper = outboxObjectMapper;
    }

    @Transactional
    public String publishTransferRequested(TransferRequest request) {
        TransferRequestedEvent event = buildEvent(request);
        String payload = serialize(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                kafkaTopicsProperties.transferRequested(),
                event.sourceAccountId(),
                payload,
                Instant.now()
        );

        outboxEventRepository.save(outboxEvent);

        log.info("Saved TransferRequestedEvent transferId={} to outbox", event.transferId());

        return event.transferId();
    }

    private TransferRequestedEvent buildEvent(TransferRequest request) {
        return new TransferRequestedEvent(
                UUID.randomUUID().toString(),
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount(),
                request.currency(),
                Instant.now()
        );
    }

    private String serialize(TransferRequestedEvent event) {
        try {
            return outboxObjectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize TransferRequestedEvent", exception);
        }
    }
}