package com.tcc.streaming.stream.infrastructure.events;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import com.tcc.streaming.stream.core.events.StreamCreatedEvent;
import com.tcc.streaming.stream.core.events.StreamEndedEvent;
import com.tcc.streaming.stream.core.events.StreamStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener para eventos de domínio de streams.
 * Publica eventos no RabbitMQ APÓS o commit da transação.
 */
@Component
public class StreamDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(StreamDomainEventListener.class);
    private final EventPublisher eventPublisher;

    public StreamDomainEventListener(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamCreated(StreamCreatedEvent event) {
        log.debug("[EventListener] Stream created event - Publishing to RabbitMQ: {}", event.streamId());
        
        try {
            eventPublisher.publishStreamCreated(
                event.streamId(),
                event.streamKey(),
                event.title()
            );
        } catch (Exception e) {
            log.error("[EventListener] Failed to publish stream_created event: {}", event, e);
            // Evento já foi commitado no BD, então apenas logamos o erro
            // Em produção, poderia reprocessar ou enviar para DLQ
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamStarted(StreamStartedEvent event) {
        log.debug("[EventListener] Stream started event - Publishing to RabbitMQ: {}", event.streamId());
        
        try {
            eventPublisher.publishStreamStarted(
                event.streamId(),
                event.streamKey()
            );
        } catch (Exception e) {
            log.error("[EventListener] Failed to publish stream_started event: {}", event, e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamEnded(StreamEndedEvent event) {
        log.debug("[EventListener] Stream ended event - Publishing to RabbitMQ: {}", event.streamId());
        
        try {
            eventPublisher.publishStreamEnded(
                event.streamId(),
                event.streamKey(),
                event.viewersPeak()
            );
        } catch (Exception e) {
            log.error("[EventListener] Failed to publish stream_ended event: {}", event, e);
        }
    }
}
