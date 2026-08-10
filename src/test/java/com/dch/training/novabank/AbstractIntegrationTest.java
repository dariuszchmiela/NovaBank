package com.dch.training.novabank;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:18-alpine";
    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.6.1";
    @Container
    @ServiceConnection
    protected static final ConfluentKafkaContainer KAFKA_CONTAINER =
            new ConfluentKafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
}