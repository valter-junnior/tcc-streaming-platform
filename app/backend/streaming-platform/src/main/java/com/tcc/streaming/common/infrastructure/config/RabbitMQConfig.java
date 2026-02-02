package com.tcc.streaming.common.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    // Exchange
    public static final String STREAM_EXCHANGE = "stream.exchange";
    
    // Queues
    public static final String STREAM_EVENTS_QUEUE = "stream.events";
    public static final String METRICS_QUEUE = "metrics.events";
    
    // Routing Keys
    public static final String STREAM_CREATED_KEY = "stream.created";
    public static final String STREAM_STARTED_KEY = "stream.started";
    public static final String STREAM_ENDED_KEY = "stream.ended";
    public static final String VIEWER_JOINED_KEY = "viewer.joined";
    public static final String VIEWER_LEFT_KEY = "viewer.left";

    @Bean
    public TopicExchange streamExchange() {
        return new TopicExchange(STREAM_EXCHANGE);
    }

    @Bean
    public Queue streamEventsQueue() {
        return QueueBuilder.durable(STREAM_EVENTS_QUEUE).build();
    }

    @Bean
    public Queue metricsQueue() {
        return QueueBuilder.durable(METRICS_QUEUE).build();
    }

    @Bean
    public Binding streamCreatedBinding() {
        return BindingBuilder
            .bind(streamEventsQueue())
            .to(streamExchange())
            .with(STREAM_CREATED_KEY);
    }

    @Bean
    public Binding streamStartedBinding() {
        return BindingBuilder
            .bind(streamEventsQueue())
            .to(streamExchange())
            .with(STREAM_STARTED_KEY);
    }

    @Bean
    public Binding streamEndedBinding() {
        return BindingBuilder
            .bind(streamEventsQueue())
            .to(streamExchange())
            .with(STREAM_ENDED_KEY);
    }

    @Bean
    public Binding metricsViewerJoinedBinding() {
        return BindingBuilder
            .bind(metricsQueue())
            .to(streamExchange())
            .with(VIEWER_JOINED_KEY);
    }

    @Bean
    public Binding metricsViewerLeftBinding() {
        return BindingBuilder
            .bind(metricsQueue())
            .to(streamExchange())
            .with(VIEWER_LEFT_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
