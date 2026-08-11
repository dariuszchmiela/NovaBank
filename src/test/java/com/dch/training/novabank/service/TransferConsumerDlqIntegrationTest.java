package com.dch.training.novabank.service;

import com.dch.training.novabank.AbstractIntegrationTest;
import com.dch.training.novabank.config.KafkaTopicsProperties;
import com.dch.training.novabank.event.TransferRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class TransferConsumerDlqIntegrationTest extends AbstractIntegrationTest {

    private static final String INVALID_TRANSFER_ID = "not-a-valid-uuid";
    private static final String SOURCE_ACCOUNT_ID = "ACC-SOURCE-DLQ-IT";
    private static final String TARGET_ACCOUNT_ID = "ACC-TARGET-DLQ-IT";
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final String CURRENCY = "PLN";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(15);

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private KafkaTopicsProperties kafkaTopicsProperties;

    private KafkaConsumer<String, String> dltConsumer;

    @BeforeEach
    void setUpDltConsumer() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "transfer-dlq-it-consumer");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        dltConsumer = new KafkaConsumer<>(consumerProperties);
        dltConsumer.subscribe(List.of(kafkaTopicsProperties.transferRequestedDlt()));
    }

    @AfterEach
    void tearDownDltConsumer() {
        dltConsumer.close();
    }

    @Test
    void shouldPublishToDltAfterProcessingKeepsFailing() {
        TransferRequestedEvent invalidEvent = new TransferRequestedEvent(
                INVALID_TRANSFER_ID, SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, AMOUNT, CURRENCY, Instant.now());

        kafkaTemplate.send(kafkaTopicsProperties.transferRequested(), SOURCE_ACCOUNT_ID, invalidEvent);

        ConsumerRecord<String, String> dltRecord = pollForRecordWithKey(SOURCE_ACCOUNT_ID);

        assertThat(dltRecord.value()).contains(INVALID_TRANSFER_ID);
    }

    private ConsumerRecord<String, String> pollForRecordWithKey(String expectedKey) {
        ConsumerRecords<String, String> records = dltConsumer.poll(POLL_TIMEOUT);

        for (ConsumerRecord<String, String> record : records.records(kafkaTopicsProperties.transferRequestedDlt())) {
            if (expectedKey.equals(record.key())) {
                return record;
            }
        }

        throw new AssertionError("No record found on DLT with key " + expectedKey);
    }
}