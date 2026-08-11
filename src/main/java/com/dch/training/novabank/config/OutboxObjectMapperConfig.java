package com.dch.training.novabank.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxObjectMapperConfig {

    @Bean
    public ObjectMapper outboxObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}