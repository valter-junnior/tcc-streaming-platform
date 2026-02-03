package com.tcc.streaming.stream.application.services;

import com.tcc.streaming.stream.core.dtos.stream.CreateStreamDto;
import com.tcc.streaming.stream.core.dtos.stream.StreamDto;
import com.tcc.streaming.stream.core.dtos.stream.StreamStatusDto;
import com.tcc.streaming.stream.core.dtos.stream.UpdateStreamDto;
import com.tcc.streaming.stream.core.entities.Stream;
import com.tcc.streaming.stream.core.entities.StreamStatus;
import com.tcc.streaming.stream.core.exceptions.StreamNotFoundException;
import com.tcc.streaming.stream.core.exceptions.UnauthorizedException;
import com.tcc.streaming.stream.core.repositories.StreamRepository;
import com.tcc.streaming.stream.core.usecases.CreateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.DeleteStreamUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamStatusUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ListLiveStreamsUseCase;
import com.tcc.streaming.stream.core.usecases.ListUserStreamsUseCase;
import com.tcc.streaming.stream.core.usecases.UpdateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ValidateStreamKeyUseCase;
import com.tcc.streaming.stream.core.events.StreamCreatedEvent;
import com.tcc.streaming.stream.core.events.StreamEndedEvent;
import com.tcc.streaming.stream.core.events.StreamStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StreamService implements CreateStreamUseCase, GetStreamUseCase, GetStreamStatusUseCase, DeleteStreamUseCase, ValidateStreamKeyUseCase, ListLiveStreamsUseCase, ListUserStreamsUseCase, UpdateStreamUseCase {

    private static final Logger log = LoggerFactory.getLogger(StreamService.class);
    private final StreamRepository streamRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public StreamService(
            StreamRepository streamRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.streamRepository = streamRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional
    public StreamDto execute(CreateStreamDto dto) {
        log.debug("[StreamService] Creating stream entity - Title: {}, OwnerId: {}", dto.title(), dto.ownerId());
        
        // Criar entidade de domínio
        Stream stream = Stream.create(dto.title(), dto.description(), dto.ownerId());
        
        // Persistir
        Stream saved = streamRepository.save(stream);
        log.debug("[StreamService] Stream persisted - ID: {}", saved.getId());
        
        // Publicar evento de domínio (será processado após commit)
        applicationEventPublisher.publishEvent(
            new StreamCreatedEvent(saved.getId(), saved.getStreamKey(), saved.getTitle())
        );
        
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StreamDto execute(UUID id) {
        log.debug("[StreamService] Fetching stream - ID: {}", id);
        Stream stream = streamRepository.findById(id)
            .orElseThrow(() -> new StreamNotFoundException(id));
        
        return toDto(stream);
    }

    @Override
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
    @Transactional
    public void deleteStream(UUID id, String ownerId) {
        Stream stream = streamRepository.findById(id)
            .orElseThrow(() -> new StreamNotFoundException(id));
        
        log.info("[StreamService] Deleting stream - ID: {}, Owner: {}, Current Status: {}", id, ownerId, stream.getStatus());
        
        // Validar ownership
        if (!stream.getOwnerId().equals(ownerId)) {
            log.warn("[StreamService] Unauthorized delete attempt - Stream: {}, Owner: {}, Requester: {}", 
                id, stream.getOwnerId(), ownerId);
            throw new UnauthorizedException("Você não tem permissão para deletar esta stream");
        }
        
        // Validar que stream não está LIVE
        if (stream.getStatus() == StreamStatus.LIVE) {
            throw new IllegalStateException("Não é possível deletar uma stream ao vivo. Finalize a transmissão primeiro.");
        }
        
        // Forçar status ENDED se necessário
        if (stream.getStatus() != StreamStatus.ENDED) {
            stream.forceEnd();
            streamRepository.save(stream);
        }
        
        // Deletar fisicamente
        streamRepository.deleteById(id);
        
        // Publicar evento de domínio (será processado após commit)
        applicationEventPublisher.publishEvent(
            new StreamEndedEvent(stream.getId(), stream.getStreamKey(), stream.getViewersPeak())
        );
        log.info("[StreamService] Stream deleted permanently - ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean execute(String streamKey) {
        return streamRepository.findByStreamKey(streamKey).isPresent();
    }

    @Transactional
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3)
    public UUID startStream(String streamKey) {
        Stream stream = streamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new StreamNotFoundException(streamKey));
        
        log.info("[StreamService] Starting stream - ID: {}, Key: {}, Status: {}", 
                 stream.getId(), streamKey, stream.getStatus());
        
        stream.start();
        streamRepository.save(stream);
        
        log.info("[StreamService] Stream started - ID: {}, New Status: {}", 
                 stream.getId(), stream.getStatus());
        
        // Publicar evento de domínio (será processado após commit)
        applicationEventPublisher.publishEvent(
            new StreamStartedEvent(stream.getId(), stream.getStreamKey())
        );
        
        return stream.getId();
    }

    @Transactional
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3)
    public UUID endStream(String streamKey) {
        Stream stream = streamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new StreamNotFoundException(streamKey));
        
        log.info("[StreamService] Ending stream - ID: {}, Key: {}, Status: {}", 
                 stream.getId(), streamKey, stream.getStatus());
        
        stream.end();
        streamRepository.save(stream);
        
        log.info("[StreamService] Stream ended - ID: {}, Peak viewers: {}", 
                 stream.getId(), stream.getViewersPeak());
        
        // Publicar evento de domínio (será processado após commit)
        applicationEventPublisher.publishEvent(
            new StreamEndedEvent(stream.getId(), stream.getStreamKey(), stream.getViewersPeak())
        );
        
        return stream.getId();
    }

    @Transactional
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
        
        // Publicar evento de domínio (será processado após commit)
        applicationEventPublisher.publishEvent(
            new StreamCreatedEvent(stream.getId(), stream.getStreamKey(), stream.getTitle())
        );
    }

    private StreamDto toDto(Stream stream) {
        return new StreamDto(
            stream.getId(),
            stream.getTitle(),
            stream.getDescription(),
            stream.getStreamKey(),
            stream.getOwnerId(),
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

    @Override
    @Transactional(readOnly = true)
    public List<StreamDto> execute() {
        log.debug("[StreamService] Fetching all LIVE streams");
        List<Stream> liveStreams = streamRepository.findByStatus(StreamStatus.LIVE);
        log.debug("[StreamService] Found {} LIVE streams", liveStreams.size());
        
        return liveStreams.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StreamDto> listByOwner(String ownerId) {
        log.debug("[StreamService] Fetching streams for owner: {}", ownerId);
        List<Stream> userStreams = streamRepository.findByOwnerId(ownerId);
        log.debug("[StreamService] Found {} streams for owner {}", userStreams.size(), ownerId);
        
        return userStreams.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StreamDto update(UpdateStreamDto dto) {
        log.debug("[StreamService] Updating stream {} for owner {}", dto.streamId(), dto.ownerId());
        
        Stream stream = streamRepository.findById(dto.streamId())
            .orElseThrow(() -> new StreamNotFoundException(dto.streamId()));
        
        // Validar ownership
        if (!stream.getOwnerId().equals(dto.ownerId())) {
            log.warn("[StreamService] Unauthorized update attempt - Stream: {}, Owner: {}, Requester: {}", 
                dto.streamId(), stream.getOwnerId(), dto.ownerId());
            throw new UnauthorizedException("Você não tem permissão para editar esta stream");
        }
        
        // Atualizar campos
        stream.setTitle(dto.title());
        stream.setDescription(dto.description());
        stream.setUpdatedAt(java.time.LocalDateTime.now());
        
        Stream updated = streamRepository.save(stream);
        log.debug("[StreamService] Stream {} updated successfully", dto.streamId());
        
        return toDto(updated);
    }
    
    /**
     * Cleanup inactive streams that haven't been updated for more than the specified threshold
     * @param thresholdDays Number of days of inactivity before cleanup
     * @return Number of streams cleaned up
     */
    @Transactional
    public int cleanupInactiveStreams(int thresholdDays) {
        java.time.LocalDateTime thresholdDate = java.time.LocalDateTime.now().minusDays(thresholdDays);
        log.info("[StreamService] Starting cleanup of inactive streams (threshold: {} days, date: {})", 
                 thresholdDays, thresholdDate);
        
        List<Stream> inactiveStreams = streamRepository.findInactiveStreams(thresholdDate);
        log.info("[StreamService] Found {} inactive streams to cleanup", inactiveStreams.size());
        
        int cleanedCount = 0;
        for (Stream stream : inactiveStreams) {
            try {
                log.info("[StreamService] Cleaning up inactive stream - ID: {}, Title: {}, Status: {}, LastUpdate: {}", 
                         stream.getId(), stream.getTitle(), stream.getStatus(), stream.getUpdatedAt());
                
                // Force end the stream
                stream.forceEnd();
                streamRepository.save(stream);
                
                // Publicar evento de domínio (será processado após commit)
                applicationEventPublisher.publishEvent(
                    new StreamEndedEvent(stream.getId(), stream.getStreamKey(), stream.getViewersPeak())
                );
                
                cleanedCount++;
            } catch (Exception e) {
                log.error("[StreamService] Error cleaning up stream {}: {}", stream.getId(), e.getMessage(), e);
            }
        }
        
        log.info("[StreamService] Cleanup completed - {} streams cleaned up", cleanedCount);
        return cleanedCount;
    }

    /**
     * Increment viewer count for a stream
     * @param streamId Stream ID
     * @return Updated stream with new viewer count
     */
    @Transactional
    public StreamDto incrementViewers(UUID streamId) {
        Stream stream = streamRepository.findById(streamId)
            .orElseThrow(() -> new StreamNotFoundException(streamId));
        
        stream.incrementViewers();
        stream.setUpdatedAt(java.time.LocalDateTime.now());
        Stream updated = streamRepository.save(stream);
        
        log.debug("[StreamService] Viewers incremented - Stream: {}, Current: {}, Peak: {}", 
                 streamId, updated.getCurrentViewers(), updated.getViewersPeak());
        
        return toDto(updated);
    }

    /**
     * Decrement viewer count for a stream
     * @param streamId Stream ID
     * @return Updated stream with new viewer count
     */
    @Transactional
    public StreamDto decrementViewers(UUID streamId) {
        Stream stream = streamRepository.findById(streamId)
            .orElseThrow(() -> new StreamNotFoundException(streamId));
        
        stream.decrementViewers();
        stream.setUpdatedAt(java.time.LocalDateTime.now());
        Stream updated = streamRepository.save(stream);
        
        log.debug("[StreamService] Viewers decremented - Stream: {}, Current: {}", 
                 streamId, updated.getCurrentViewers());
        
        return toDto(updated);
    }
}

