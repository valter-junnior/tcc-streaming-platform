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
        log.debug("[StreamService] Creating stream entity - Title: {}", dto.title());
        
        // Criar entidade de domínio
        Stream stream = Stream.create(dto.title(), dto.description());
        
        // Persistir
        Stream saved = streamRepository.save(stream);
        log.debug("[StreamService] Stream persisted - ID: {}", saved.getId());
        
        // Publicar evento stream_created no RabbitMQ
        eventPublisher.publishStreamCreated(saved.getId(), saved.getStreamKey(), saved.getTitle());
        
        return toDto(saved);
    }

    @Override
    @Cacheable(value = "streams", key = "#id")
    @Transactional(readOnly = true)
    public StreamDto execute(UUID id) {
        log.debug("[StreamService] Fetching stream - ID: {}", id);
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
        
        log.info("[StreamService] Deleting stream - ID: {}, Current Status: {}", id, stream.getStatus());
        
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
        log.info("[StreamService] Stream deleted - ID: {}", id);
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
        
        log.info("[StreamService] Starting stream - ID: {}, Key: {}, Status: {}", 
                 stream.getId(), streamKey, stream.getStatus());
        
        stream.start();
        streamRepository.save(stream);
        
        log.info("[StreamService] Stream started - ID: {}, New Status: {}", 
                 stream.getId(), stream.getStatus());
        
        // Publicar evento stream_started no RabbitMQ
        eventPublisher.publishStreamStarted(stream.getId(), stream.getStreamKey());
        
        return stream.getId();
    }

    @Transactional
    @CacheEvict(value = {"streams", "streamStatus"}, key = "#result")
    public UUID endStream(String streamKey) {
        Stream stream = streamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new StreamNotFoundException(streamKey));
        
        log.info("[StreamService] Ending stream - ID: {}, Key: {}, Status: {}", 
                 stream.getId(), streamKey, stream.getStatus());
        
        stream.end();
        streamRepository.save(stream);
        
        log.info("[StreamService] Stream ended - ID: {}, Peak viewers: {}", 
                 stream.getId(), stream.getViewersPeak());
        
        // Publicar evento stream_ended no RabbitMQ
        eventPublisher.publishStreamEnded(stream.getId(), stream.getStreamKey(), stream.getViewersPeak());
        
        return stream.getId();
    }

    @Transactional
    @CacheEvict(value = {"streams", "streamStatus"}, key = "#id")
    public void restartStream(UUID id) {
        Stream stream = streamRepository.findById(id)
            .orElseThrow(() -> new StreamNotFoundException(id));
        
        log.info("[StreamService] Restarting stream - ID: {}, Current Status: {}", 
                 stream.getId(), stream.getStatus());
        
        // Permitir reiniciar apenas streams ENDED
        if (stream.getStatus() != StreamStatus.ENDED) {
            log.warn("[StreamService] Cannot restart stream - ID: {}, Status: {}", id, stream.getStatus());
            throw new IllegalStateException("Only ENDED streams can be restarted");
        }
        
        stream.restart();
        streamRepository.save(stream);
        
        log.info("[StreamService] Stream restarted - ID: {}, New Status: {}", 
                 stream.getId(), stream.getStatus());
        
        // Publicar evento stream_restarted no RabbitMQ
        eventPublisher.publishStreamCreated(stream.getId(), stream.getStreamKey(), stream.getTitle());
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
