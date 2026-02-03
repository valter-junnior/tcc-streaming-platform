package com.tcc.streaming.consumer.infrastructure.messaging;

import com.tcc.streaming.consumer.core.dtos.StreamEventDto;
import com.tcc.streaming.consumer.core.usecases.ProcessStreamEventUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Consumer de eventos de stream do RabbitMQ
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.host", matchIfMissing = false)
public class StreamEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StreamEventConsumer.class);
    private final ProcessStreamEventUseCase processStreamEventUseCase;

    public StreamEventConsumer(ProcessStreamEventUseCase processStreamEventUseCase) {
        this.processStreamEventUseCase = processStreamEventUseCase;
    }

    /**
     * Consome eventos stream_created
     */
    @RabbitListener(queues = "stream.events", containerFactory = "rabbitListenerContainerFactory")
    public void handleStreamEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");
            log.debug("[StreamEventConsumer] Received event: {}", eventType);
            
            // Extrair dados do evento
            UUID streamId = UUID.fromString((String) message.get("streamId"));
            String streamKey = (String) message.get("streamKey");
            String title = (String) message.get("title");
            Integer viewersPeak = (Integer) message.get("viewersPeak");
            String viewerId = (String) message.get("viewerId");
            
            // Criar DTO
            StreamEventDto eventDto = new StreamEventDto(
                streamId,
                eventType,
                streamKey,
                title,
                viewersPeak,
                viewerId,
                LocalDateTime.now()
            );
            
            // Processar evento
            processStreamEventUseCase.execute(eventDto);
            
            // Lógica específica por tipo de evento
            switch (eventType) {
                case "stream_created" -> handleStreamCreated(streamId, title);
                case "stream_started" -> handleStreamStarted(streamId, streamKey);
                case "stream_ended" -> handleStreamEnded(streamId, viewersPeak);
                default -> log.debug("[StreamEventConsumer] Event type {} processed", eventType);
            }
            
        } catch (Exception e) {
            log.error("[StreamEventConsumer] Error processing stream event: {}", e.getMessage(), e);
            throw e; // Permite retry do RabbitMQ
        }
    }
    
    private void handleStreamCreated(UUID streamId, String title) {
        log.info("[StreamEventConsumer] Stream created - ID: {}, Title: {}", streamId, title);
        // Registrar métricas iniciais
        // Pode adicionar lógica adicional aqui
    }
    
    private void handleStreamStarted(UUID streamId, String streamKey) {
        log.info("[StreamEventConsumer] Stream started - ID: {}, Key: {}", streamId, streamKey);
        // Iniciar coleta de métricas
        // Pode adicionar lógica adicional aqui
    }
    
    private void handleStreamEnded(UUID streamId, Integer viewersPeak) {
        log.info("[StreamEventConsumer] Stream ended - ID: {}, Peak viewers: {}", streamId, viewersPeak);
        // Calcular estatísticas finais
        // Pode adicionar lógica adicional aqui
    }
}
