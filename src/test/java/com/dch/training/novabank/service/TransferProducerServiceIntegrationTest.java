package com.dch.training.novabank.service;

import com.dch.training.novabank.config.KafkaTopicsProperties;
import com.dch.training.novabank.dto.TransferRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class TransferProducerServiceIntegrationTest {

    @Container
    @ServiceConnection
    static final ConfluentKafkaContainer KAFKA_CONTAINER =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
    private static final String SOURCE_ACCOUNT_ID = "ACC-SOURCE-IT";
    private static final String TARGET_ACCOUNT_ID = "ACC-TARGET-IT";
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");
    private static final String CURRENCY = "PLN";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(10);
    @Autowired
    private TransferProducerService transferProducerService;

    @Autowired
    private KafkaTopicsProperties kafkaTopicsProperties;

    private KafkaConsumer<String, String> testConsumer;

    @BeforeEach
    void setUpConsumer() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "transfer-producer-it-consumer");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        testConsumer = new KafkaConsumer<>(consumerProperties);
        testConsumer.subscribe(List.of(kafkaTopicsProperties.transferRequested()));
    }

    @AfterEach
    void tearDownConsumer() {
        testConsumer.close();
    }

    @Test
    void publishTransferRequested_shouldActuallyDeliverEventToKafkaTopic() {
        TransferRequest request = new TransferRequest(SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, AMOUNT, CURRENCY);

        String transferId = transferProducerService.publishTransferRequested(request);

        ConsumerRecord<String, String> receivedRecord = pollForRecordWithKey(SOURCE_ACCOUNT_ID);

        assertThat(receivedRecord.key()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(receivedRecord.value()).contains(transferId);
        assertThat(receivedRecord.value()).contains(TARGET_ACCOUNT_ID);
    }

    private ConsumerRecord<String, String> pollForRecordWithKey(String expectedKey) {
        ConsumerRecords<String, String> records = testConsumer.poll(POLL_TIMEOUT);

        for (ConsumerRecord<String, String> record : records.records(kafkaTopicsProperties.transferRequested())) {
            if (expectedKey.equals(record.key())) {
                return record;
            }
        }

        throw new AssertionError("No record found with key " + expectedKey);
    }
}