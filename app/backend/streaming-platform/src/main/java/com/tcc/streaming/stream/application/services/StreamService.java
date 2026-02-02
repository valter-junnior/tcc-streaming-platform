package com.tcc.streaming.stream.application.services;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import com.tcc.streaming.common.infrastructure.rtmp.RtmpServerGateway;
import com.tcc.streaming.stream.core.dtos.stream.CreateStreamDto;
import com.tcc.streaming.stream.core.dtos.stream.StreamDto;
import com.tcc.streaming.stream.core.dtos.stream.StreamStatusDto;
import com.tcc.streaming.stream.core.entities.Stream;
import com.tcc.streaming.stream.core.entities.StreamStatus;
import com.tcc.streaming.stream.core.exceptions.StreamNotFoundException;
import com.tcc.streaming.stream.core.repositories.StreamRepository;
import com.tcc.streaming.stream.core.usecases.CreateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.DeleteStreamUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamStatusUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ValidateStreamKeyUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StreamService implements CreateStreamUseCase, GetStreamUseCase, GetStreamStatusUseCase, DeleteStreamUseCase, ValidateStreamKeyUseCase {

    private static final Logger log = LoggerFactory.getLogger(StreamService.class);
    private final StreamRepository streamRepository;
    private final EventPublisher eventPublisher;
    private final RtmpServerGateway rtmpServerGateway;

    public StreamService(
            StreamRepository streamRepository, 
            EventPublisher eventPublisher,
            RtmpServerGateway rtmpServerGateway) {
        this.streamRepository = streamRepository;
        this.eventPublisher = eventPublisher;
        this.rtmpServerGateway = rtmpServerGateway;
    }

    @Override
    @Transactional
    @CacheEvict(value = "streams", key = "#result.id")
    public StreamDto execute(CreateStreamDto dto) {
        // Get RTMP configuration from the configured gateway
        String rtmpUrl = rtmpServerGateway.getServerConfig().getCompleteRtmpUrl();
        
        // Criar entidade de domínio
        Stream stream = Stream.create(dto.title(), dto.description());
        
        // Persistir
        Stream saved = streamRepository.save(stream);
        
        // Publicar evento stream_created no RabbitMQ
        eventPublisher.publishStreamCreated(saved.getId(), saved.getStreamKey(), saved.getTitle());
        
        return toDto(saved);
    }

    @Override
    @Cacheable(value = "streams", key = "#id")
    @Transactional(readOnly = true)
    public StreamDto execute(UUID id) {
        Stream stream = streamRepository.findById(id)
            .orElseThrow(() -> new StreamNotFoundException(id));
        
        return toDto(stream);
    }

    @Override
    @Cacheable(value = "streamStatus", key = "#id")
    @Transactional(readOnly = true)
    public StreamStatusDto getStatus(UUID id) {
        Stream stream = streamRepository.findById(id)
            .orElseThrow(() -> new StreamNotFoundException(id));
        
        return new StreamStatusDto(
            stream.getId(),
            stream.getStatus(),
            stream.getCurrentViewers(),
            stream.getViewersPeak(),
            stream.getWatchUrl()
        );
    }

    @Override
    @CacheEvict(value = "streams", key = "#id")
    @Transactional
    public void deleteStream(UUID id) {
        Stream stream = streamRepository.findById(id)
            .orElseThrow(() -> new StreamNotFoundException(id));
        
        // Only call end() if stream is LIVE, otherwise just mark as ENDED directly
        if (stream.getStatus() == StreamStatus.LIVE) {
            stream.end();
        } else if (stream.getStatus() != StreamStatus.ENDED) {
            // If not LIVE and not already ENDED, force status to ENDED
            stream.forceEnd();
        }
        
        streamRepository.save(stream);
        
        // Publicar evento stream_ended no RabbitMQ
        eventPublisher.publishStreamEnded(stream.getId(), stream.getStreamKey(), stream.getViewersPeak());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean execute(String streamKey) {
        return streamRepository.findByStreamKey(streamKey).isPresent();
    }

    @Transactional
    @CacheEvict(value = {"streams", "streamStatus"}, key = "#result")
    public UUID startStream(String streamKey) {
        Stream stream = streamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new StreamNotFoundException(streamKey));
        
        log.info("[START] Attempting to start stream - ID: {}, Title: '{}', Current Status: {}", 
                 stream.getId(), stream.getTitle(), stream.getStatus());
        
        stream.start();
        streamRepository.save(stream);
        
        log.info("[START] Stream started successfully - ID: {}, Title: '{}', New Status: {}", 
                 stream.getId(), stream.getTitle(), stream.getStatus());
        
        // Publicar evento stream_started no RabbitMQ
        eventPublisher.publishStreamStarted(stream.getId(), stream.getStreamKey());
        
        return stream.getId();
    }

    @Transactional
    @CacheEvict(value = {"streams", "streamStatus"}, key = "#result")
    public UUID endStream(String streamKey) {
        Stream stream = streamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new StreamNotFoundException(streamKey));
        
        log.info("[END] Attempting to end stream - ID: {}, Title: '{}', Current Status: {}", 
                 stream.getId(), stream.getTitle(), stream.getStatus());
        
        stream.end();
        streamRepository.save(stream);
        
        log.info("[END] Stream ended successfully - ID: {}, Title: '{}', New Status: {}, Peak viewers: {}", 
                 stream.getId(), stream.getTitle(), stream.getStatus(), stream.getViewersPeak());
        
        // Publicar evento stream_ended no RabbitMQ
        eventPublisher.publishStreamEnded(stream.getId(), stream.getStreamKey(), stream.getViewersPeak());
        
        return stream.getId();
    }

    private StreamDto toDto(Stream stream) {
        return new StreamDto(
            stream.getId(),
            stream.getTitle(),
            stream.getDescription(),
            stream.getStreamKey(),
            stream.getStatus(),
            stream.getCreatedAt(),
            stream.getStartedAt(),
            stream.getEndedAt(),
            stream.getCurrentViewers(),
            stream.getViewersPeak(),
            stream.getRtmpUrl(),
            stream.getWatchUrl()
        );
    }
}
