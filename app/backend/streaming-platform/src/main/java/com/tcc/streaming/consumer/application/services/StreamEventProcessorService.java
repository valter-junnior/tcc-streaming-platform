package com.tcc.streaming.consumer.application.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public StreamEventProcessorService(StreamEventRepository streamEventRepository, ObjectMapper objectMapper) {
        this.streamEventRepository = streamEventRepository;
        this.objectMapper = objectMapper;
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
        try {
            return objectMapper.writeValueAsString(eventDto);
        } catch (JsonProcessingException e) {
            log.error("[StreamEventProcessor] Failed to serialize metadata for event: {}", eventDto, e);
            throw new RuntimeException("Failed to serialize event metadata", e);
        }
    }
}
