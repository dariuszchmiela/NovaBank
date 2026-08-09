package com.dch.trining.novabank.service;

import com.dch.trining.novabank.config.KafkaTopicsProperties;
import com.dch.trining.novabank.dto.TransferRequest;
import com.dch.trining.novabank.event.TransferRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransferProducerService {
    private static final Logger log = LoggerFactory.getLogger(TransferProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    public TransferProducerService(KafkaTemplate<String, Object> kafkaTemplate,
                                   KafkaTopicsProperties kafkaTopicsProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopicsProperties = kafkaTopicsProperties;
    }

    public String publishTransferRequested(TransferRequest request) {
        TransferRequestedEvent event = buildEvent(request);
        String partitionKey = event.sourceAccountId();
        log.info("Publishing TransferRequestedEvent transferId={} sourceAccountId={}",
                event.transferId(), partitionKey);
        kafkaTemplate.send(kafkaTopicsProperties.transferRequested(), partitionKey, event);
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
}