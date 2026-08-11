package com.dch.training.novabank.service;

import com.dch.training.novabank.config.OutboxProperties;
import com.dch.training.novabank.entity.OutboxEvent;
import com.dch.training.novabank.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> outboxKafkaTemplate;
    private final OutboxProperties outboxProperties;

    public OutboxPublisherScheduler(OutboxEventRepository outboxEventRepository,
                                    @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> outboxKafkaTemplate,
                                    OutboxProperties outboxProperties) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxKafkaTemplate = outboxKafkaTemplate;
        this.outboxProperties = outboxProperties;
    }

    @Scheduled(fixedDelayString = "${novabank.outbox.poll-interval-ms}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent outboxEvent : pendingEvents) {
            publishEvent(outboxEvent);
        }
    }

    private void publishEvent(OutboxEvent outboxEvent) {
        try {
            outboxKafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getMessageKey(), outboxEvent.getPayload())
                    .get(outboxProperties.publishTimeoutSeconds(), TimeUnit.SECONDS);

            outboxEvent.markPublished(Instant.now());
            outboxEventRepository.save(outboxEvent);

            log.info("Published outbox event id={} topic={}", outboxEvent.getId(), outboxEvent.getTopic());
        } catch (Exception exception) {
            log.warn("Failed to publish outbox event id={}, will retry on next poll", outboxEvent.getId(), exception);
        }
    }
}