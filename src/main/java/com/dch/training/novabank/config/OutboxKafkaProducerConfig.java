package com.dch.training.novabank.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
public class OutboxKafkaProducerConfig {

    private final KafkaProperties kafkaProperties;
    private final KafkaConnectionDetails kafkaConnectionDetails;

    public OutboxKafkaProducerConfig(KafkaProperties kafkaProperties,
                                     KafkaConnectionDetails kafkaConnectionDetails) {
        this.kafkaProperties = kafkaProperties;
        this.kafkaConnectionDetails = kafkaConnectionDetails;
    }

    @Bean
    @Primary
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        return new KafkaTemplate<>(buildProducerFactory(buildBaseProperties()));
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate() {
        Map<String, Object> properties = buildBaseProperties();
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(buildProducerFactory(properties));
    }

    private Map<String, Object> buildBaseProperties() {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getBootstrapServers());
        return properties;
    }

    private <K, V> ProducerFactory<K, V> buildProducerFactory(Map<String, Object> properties) {
        return new DefaultKafkaProducerFactory<>(properties);
    }
}