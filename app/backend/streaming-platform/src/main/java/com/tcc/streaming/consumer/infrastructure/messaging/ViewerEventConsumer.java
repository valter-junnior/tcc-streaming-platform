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
 * Consumer de eventos de viewers (métricas) do RabbitMQ
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.host", matchIfMissing = false)
public class ViewerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ViewerEventConsumer.class);
    private final ProcessStreamEventUseCase processStreamEventUseCase;

    public ViewerEventConsumer(ProcessStreamEventUseCase processStreamEventUseCase) {
        this.processStreamEventUseCase = processStreamEventUseCase;
    }

    /**
     * Consome eventos viewer_joined e viewer_left
     */
    @RabbitListener(queues = "metrics.events", containerFactory = "rabbitListenerContainerFactory")
    public void handleViewerEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");
            UUID streamId = UUID.fromString((String) message.get("streamId"));
            String viewerId = (String) message.get("viewerId");
            
            StreamEventDto eventDto = new StreamEventDto(
                streamId,
                eventType,
                null,  // streamKey
                null,  // title
                null,  // viewersPeak
                viewerId,
                LocalDateTime.now()
            );
            
            // Processar evento
            processStreamEventUseCase.execute(eventDto);
            
            // Lógica específica por tipo de evento
            if ("viewer_joined".equals(eventType)) {
                handleViewerJoined(streamId, viewerId);
            } else if ("viewer_left".equals(eventType)) {
                handleViewerLeft(streamId, viewerId);
            }
            
        } catch (Exception e) {
            log.error("[ViewerEventConsumer] Error processing viewer event: {}", e.getMessage(), e);
            throw e; // Permite retry do RabbitMQ
        }
    }
    
    private void handleViewerJoined(UUID streamId, String viewerId) {
        log.info("[ViewerEventConsumer] Viewer joined - StreamId: {}, ViewerId: {}", streamId, viewerId);
        // Incrementar contador de viewers
        // Atualizar métricas de pico se necessário
    }
    
    private void handleViewerLeft(UUID streamId, String viewerId) {
        log.info("[ViewerEventConsumer] Viewer left - StreamId: {}, ViewerId: {}", streamId, viewerId);
        // Decrementar contador de viewers
        // Registrar duração da sessão
    }
}
