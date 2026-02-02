package com.tcc.streaming.stream.application.services;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import com.tcc.streaming.stream.core.dtos.stream.CreateStreamDto;
import com.tcc.streaming.stream.core.dtos.stream.StreamDto;
import com.tcc.streaming.stream.core.dtos.stream.StreamStatusDto;
import com.tcc.streaming.stream.core.entities.Stream;
import com.tcc.streaming.stream.core.exceptions.StreamNotFoundException;
import com.tcc.streaming.stream.core.repositories.StreamRepository;
import com.tcc.streaming.stream.core.usecases.CreateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.DeleteStreamUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamStatusUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ValidateStreamKeyUseCase;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StreamService implements CreateStreamUseCase, GetStreamUseCase, GetStreamStatusUseCase, DeleteStreamUseCase, ValidateStreamKeyUseCase {

    private final StreamRepository streamRepository;
    private final EventPublisher eventPublisher;

    public StreamService(StreamRepository streamRepository, EventPublisher eventPublisher) {
        this.streamRepository = streamRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @CacheEvict(value = "streams", key = "#result.id")
    public StreamDto execute(CreateStreamDto dto) {
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
        
        stream.end();
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
    public void startStream(String streamKey) {
        Stream stream = streamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new StreamNotFoundException(streamKey));
        
        stream.start();
        streamRepository.save(stream);
        
        // Publicar evento stream_started no RabbitMQ
        eventPublisher.publishStreamStarted(stream.getId(), stream.getStreamKey());
    }

    @Transactional
    public void endStream(String streamKey) {
        Stream stream = streamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new StreamNotFoundException(streamKey));
        
        stream.end();
        streamRepository.save(stream);
        
        // Publicar evento stream_ended no RabbitMQ
        eventPublisher.publishStreamEnded(stream.getId(), stream.getStreamKey(), stream.getViewersPeak());
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
