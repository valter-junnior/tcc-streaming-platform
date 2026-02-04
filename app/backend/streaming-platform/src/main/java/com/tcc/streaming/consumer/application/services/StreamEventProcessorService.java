package com.tcc.streaming.consumer.application.services;

import com.tcc.streaming.consumer.core.dtos.StreamEventDto;
import com.tcc.streaming.consumer.core.usecases.ProcessStreamEventUseCase;
import com.tcc.streaming.stream.core.entities.StreamEvent;
import com.tcc.streaming.stream.core.entities.StreamEventType;
import com.tcc.streaming.stream.core.repositories.StreamEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para processar eventos de stream do message broker
 */
@Service
public class StreamEventProcessorService implements ProcessStreamEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(StreamEventProcessorService.class);
    private final StreamEventRepository streamEventRepository;

    public StreamEventProcessorService(StreamEventRepository streamEventRepository) {
        this.streamEventRepository = streamEventRepository;
    }

    @Override
    @Transactional
    public void execute(StreamEventDto eventDto) {
        try {
            StreamEventType eventType = mapEventType(eventDto.eventType());
            
            // Construir metadata JSON
            String metadata = buildMetadata(eventDto);
            
            // Criar e persistir evento
            StreamEvent event = StreamEvent.create(eventDto.streamId(), eventType, metadata);
            streamEventRepository.save(event);
            
            log.info("[StreamEventProcessor] Event persisted - Type: {}, StreamId: {}", 
                     eventType, eventDto.streamId());
            
        } catch (Exception e) {
            log.error("[StreamEventProcessor] Error processing event: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private StreamEventType mapEventType(String eventType) {
        return switch (eventType) {
            case "stream_created" -> StreamEventType.CREATED;
            case "stream_started" -> StreamEventType.STARTED;
            case "stream_ended" -> StreamEventType.ENDED;
            case "viewer_joined" -> StreamEventType.VIEWER_JOINED;
            case "viewer_left" -> StreamEventType.VIEWER_LEFT;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
    
    private String buildMetadata(StreamEventDto eventDto) {
        StringBuilder json = new StringBuilder("{");
        
        if (eventDto.streamKey() != null) {
            json.append("\"streamKey\":\"").append(eventDto.streamKey()).append("\",");
        }
        if (eventDto.title() != null) {
            json.append("\"title\":\"").append(eventDto.title()).append("\",");
        }
        if (eventDto.viewersPeak() != null) {
            json.append("\"viewersPeak\":").append(eventDto.viewersPeak()).append(",");
        }
        if (eventDto.viewerId() != null) {
            json.append("\"viewerId\":\"").append(eventDto.viewerId()).append("\",");
        }
        
        // Remove trailing comma
        if (json.length() > 1 && json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }
        
        json.append("}");
        return json.toString();
    }
}
