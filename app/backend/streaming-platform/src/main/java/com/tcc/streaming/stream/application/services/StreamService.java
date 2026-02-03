package com.tcc.streaming.stream.application.services;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import com.tcc.streaming.common.infrastructure.rtmp.RtmpServerGateway;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StreamService implements CreateStreamUseCase, GetStreamUseCase, GetStreamStatusUseCase, DeleteStreamUseCase, ValidateStreamKeyUseCase, ListLiveStreamsUseCase, ListUserStreamsUseCase, UpdateStreamUseCase {

    private static final Logger log = LoggerFactory.getLogger(StreamService.class);
    private final StreamRepository streamRepository;
    private final EventPublisher eventPublisher;
    private final RtmpServerGateway rtmpServerGateway;
    private final CacheManager cacheManager;

    public StreamService(
            StreamRepository streamRepository, 
            EventPublisher eventPublisher,
            RtmpServerGateway rtmpServerGateway,
            CacheManager cacheManager) {
        this.streamRepository = streamRepository;
        this.eventPublisher = eventPublisher;
        this.rtmpServerGateway = rtmpServerGateway;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    @CacheEvict(value = "streams", key = "#result.id")
    public StreamDto execute(CreateStreamDto dto) {
        log.debug("[StreamService] Creating stream entity - Title: {}, OwnerId: {}", dto.title(), dto.ownerId());
        
        // Criar entidade de domínio
        Stream stream = Stream.create(dto.title(), dto.description(), dto.ownerId());
        
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
        
        // Publicar evento stream_ended no RabbitMQ
        eventPublisher.publishStreamEnded(stream.getId(), stream.getStreamKey(), stream.getViewersPeak());
        
        // Deletar fisicamente
        streamRepository.deleteById(id);
        log.info("[StreamService] Stream deleted permanently - ID: {}", id);
        
        // Cache eviction após transação bem-sucedida (via TransactionSynchronization)
        evictCacheAfterCommit();
    }
    
    private void evictCacheAfterCommit() {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheManager.getCache("streams").clear();
                    cacheManager.getCache("liveStreams").clear();
                }
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean execute(String streamKey) {
        return streamRepository.findByStreamKey(streamKey).isPresent();
    }

    @Transactional
    @CacheEvict(value = {"streams", "streamStatus", "liveStreams"}, allEntries = true)
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
    @CacheEvict(value = {"streams", "streamStatus", "liveStreams"}, allEntries = true)
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
    @CacheEvict(value = {"streams", "streamStatus", "liveStreams"}, allEntries = true)
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
    @Cacheable(value = "liveStreams")
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
        
        // Cache eviction após transação bem-sucedida (via TransactionSynchronization)
        evictCacheAfterCommit();
        
        return toDto(updated);
    }
    
    /**
     * Cleanup inactive streams that haven't been updated for more than the specified threshold
     * @param thresholdDays Number of days of inactivity before cleanup
     * @return Number of streams cleaned up
     */
    @Transactional
    @CacheEvict(value = {"streams", "streamStatus", "liveStreams"}, allEntries = true)
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
                
                // Publicar evento stream_ended
                eventPublisher.publishStreamEnded(stream.getId(), stream.getStreamKey(), stream.getViewersPeak());
                
                cleanedCount++;
            } catch (Exception e) {
                log.error("[StreamService] Error cleaning up stream {}: {}", stream.getId(), e.getMessage(), e);
            }
        }
        
        log.info("[StreamService] Cleanup completed - {} streams cleaned up", cleanedCount);
        return cleanedCount;
    }
}

