package com.tcc.streaming.config;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.*;

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public EventPublisher eventPublisher() {
        EventPublisher mock = Mockito.mock(EventPublisher.class);
        
        // Configure all methods to do nothing (avoid NullPointerException or exceptions)
        Mockito.doNothing().when(mock).publishStreamCreated(any(), anyString(), anyString());
        Mockito.doNothing().when(mock).publishStreamStarted(any(), anyString());
        Mockito.doNothing().when(mock).publishStreamEnded(any(), anyString(), anyInt());
        
        return mock;
    }
}
