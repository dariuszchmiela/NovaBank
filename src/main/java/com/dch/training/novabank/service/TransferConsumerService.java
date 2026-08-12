package com.dch.training.novabank.service;

import com.dch.training.novabank.entity.ProcessedTransfer;
import com.dch.training.novabank.event.TransferRequestedEvent;
import com.dch.training.novabank.repository.ProcessedTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransferConsumerService {

    private static final Logger log = LoggerFactory.getLogger(TransferConsumerService.class);

    private final ProcessedTransferRepository processedTransferRepository;

    public TransferConsumerService(ProcessedTransferRepository processedTransferRepository) {
        this.processedTransferRepository = processedTransferRepository;
    }

    @KafkaListener(topics = "${novabank.kafka.topics.transfer-requested}")
    public void consumeTransferRequested(TransferRequestedEvent event, Acknowledgment acknowledgment) {
        processTransfer(event);
        acknowledgment.acknowledge();
    }

    private void processTransfer(TransferRequestedEvent event) {
        UUID transferId = UUID.fromString(event.transferId());

        try {
            processedTransferRepository.save(toEntity(event, transferId));
            log.info("Processed transferId={} sourceAccountId={}", event.transferId(), event.sourceAccountId());
        } catch (DataIntegrityViolationException exception) {
            if (processedTransferRepository.existsById(transferId)) {
                log.info("Skipping duplicate transferId={} — already processed", event.transferId());
            } else {
                throw exception;
            }
        }
    }

    private ProcessedTransfer toEntity(TransferRequestedEvent event, UUID transferId) {
        return new ProcessedTransfer(
                transferId,
                event.sourceAccountId(),
                event.targetAccountId(),
                event.amount(),
                event.currency(),
                Instant.now()
        );
    }
}