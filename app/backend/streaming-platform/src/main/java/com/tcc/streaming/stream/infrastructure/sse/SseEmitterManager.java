package com.tcc.streaming.stream.infrastructure.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.streaming.stream.infrastructure.config.SseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Component
public class SseEmitterManager {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterManager.class);
    
    private final ScheduledExecutorService keepaliveScheduler = Executors.newScheduledThreadPool(1);
    private final ObjectMapper objectMapper;
    private final SseProperties sseProperties;
    
    private final Map<UUID, Set<ViewerEmitter>> streamEmitters = new ConcurrentHashMap<>();
    private final Map<String, ViewerEmitter> viewerEmitters = new ConcurrentHashMap<>();
    private final Map<String, UUID> activeViewers = new ConcurrentHashMap<>();

    public SseEmitterManager(ObjectMapper objectMapper, SseProperties sseProperties) {
        this.objectMapper = objectMapper;
        this.sseProperties = sseProperties;
        
        // Keepalive task
        keepaliveScheduler.scheduleAtFixedRate(() -> {
            try {
                sendKeepaliveToAll();
            } catch (Exception e) {
                log.error("[SSE] Error in keepalive task", e);
            }
        }, sseProperties.getKeepaliveInterval(), sseProperties.getKeepaliveInterval(), TimeUnit.MILLISECONDS);
        
        // TTL cleanup task
        keepaliveScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredEmitters();
            } catch (Exception e) {
                log.error("[SSE] Error in TTL cleanup task", e);
            }
        }, sseProperties.getTtlCleanupInterval(), sseProperties.getTtlCleanupInterval(), TimeUnit.MILLISECONDS);
        
        log.info("[SSE] Scheduled tasks started - Keepalive: {}s, TTL Cleanup: {}min", 
                 sseProperties.getKeepaliveInterval() / 1000, 
                 sseProperties.getTtlCleanupInterval() / 1000 / 60);
    }

    public SseEmitter createEmitter(UUID streamId, String viewerId, Runnable onDisconnect) {
        if (viewerEmitters.containsKey(viewerId)) {
            log.info("[SSE] Removing existing emitter before creating new one - ViewerId: {}", viewerId);
            removeEmitterInternal(viewerId);
        }
        
        SseEmitter emitter = new SseEmitter(sseProperties.getTimeout());
        ViewerEmitter viewerEmitter = new ViewerEmitter(streamId, viewerId, emitter, onDisconnect, LocalDateTime.now());
        
        streamEmitters.computeIfAbsent(streamId, k -> new CopyOnWriteArraySet<>()).add(viewerEmitter);
        viewerEmitters.put(viewerId, viewerEmitter);
        
        try {
            emitter.send(SseEmitter.event()
                .comment("Connected to stream " + streamId)
                .build());
        } catch (IOException e) {
            log.warn("[SSE] Failed to send initial keepalive - ViewerId: {}", viewerId);
        }
        
        log.info("[SSE] Registered emitter - StreamId: {}, ViewerId: {}, Total viewers: {}", 
                 streamId, viewerId, streamEmitters.get(streamId).size());
        
        final boolean[] callbackExecuted = {false};
        
        Runnable safeDisconnect = () -> {
            synchronized (callbackExecuted) {
                if (!callbackExecuted[0]) {
                    callbackExecuted[0] = true;
                    ViewerEmitter current = viewerEmitters.get(viewerId);
                    if (current == viewerEmitter) {
                        // Este ainda é o emitter ativo — remover e notificar
                        removeEmitterInternal(viewerId);
                        onDisconnect.run();
                    } else {
                        // Emitter foi substituído por reconexão — ignorar para não decrementar duas vezes
                        log.debug("[SSE] Stale emitter callback suppressed (viewer reconnected) - ViewerId: {}", viewerId);
                    }
                }
            }
        };
        
        emitter.onCompletion(() -> {
            log.info("[SSE] Emitter completed - StreamId: {}, ViewerId: {}, executing callback", streamId, viewerId);
            safeDisconnect.run();
        });
        
        emitter.onTimeout(() -> {
            log.info("[SSE] Emitter timeout - StreamId: {}, ViewerId: {}, executing callback", streamId, viewerId);
            safeDisconnect.run();
        });
        
        emitter.onError((ex) -> {
            log.info("[SSE] Emitter error - StreamId: {}, ViewerId: {}, Error: {}", 
                     streamId, viewerId, ex != null ? ex.getMessage() : "null");
            safeDisconnect.run();
        });
        
        return emitter;
    }

    public void removeEmitter(String viewerId) {
        removeEmitterInternal(viewerId);
    }
    
    private void removeEmitterInternal(String viewerId) {
        ViewerEmitter viewerEmitter = viewerEmitters.remove(viewerId);
        if (viewerEmitter != null) {
            Set<ViewerEmitter> emitters = streamEmitters.get(viewerEmitter.streamId());
            if (emitters != null) {
                emitters.remove(viewerEmitter);
                
                if (emitters.isEmpty()) {
                    streamEmitters.remove(viewerEmitter.streamId());
                }
            }
        }
    }

    public void broadcastToStream(UUID streamId, String eventType, Object data) {
        Set<ViewerEmitter> emitters = streamEmitters.get(streamId);
        if (emitters == null || emitters.isEmpty()) {
            log.warn("[SSE] No emitters for stream: {} - Cannot broadcast {}", streamId, eventType);
            return;
        }
        
        log.info("[SSE] Broadcasting to {} viewers - StreamId: {}, Event: {}, Data: {}", 
                  emitters.size(), streamId, eventType, data);
        
        List<String> failedViewers = new ArrayList<>();
        
        for (ViewerEmitter viewerEmitter : emitters) {
            try {
                String json = objectMapper.writeValueAsString(data);
                viewerEmitter.emitter().send(SseEmitter.event()
                    .name(eventType)
                    .data(json));
            } catch (IOException e) {
                log.warn("[SSE] Failed to send to viewer: {} - Error: {}", 
                         viewerEmitter.viewerId(), e.getMessage());
                failedViewers.add(viewerEmitter.viewerId());
            } catch (IllegalStateException e) {
                log.warn("[SSE] Emitter already completed for viewer: {}", viewerEmitter.viewerId());
                failedViewers.add(viewerEmitter.viewerId());
            }
        }
        
        failedViewers.forEach(this::removeEmitterInternal);
    }

    public void removeAllEmittersForStream(UUID streamId) {
        Set<ViewerEmitter> emitters = streamEmitters.remove(streamId);
        if (emitters != null) {
            emitters.forEach(ve -> {
                // Chamar onDisconnect explicitamente antes de remover do mapa
                // para garantir que o contador de viewers seja decrementado
                viewerEmitters.remove(ve.viewerId());
                try {
                    ve.onDisconnect().run();
                } catch (Exception e) {
                    log.debug("[SSE] Error calling onDisconnect for viewer {}: {}", ve.viewerId(), e.getMessage());
                }
                try {
                    ve.emitter().complete();
                } catch (Exception e) {
                    log.debug("[SSE] Error completing emitter: {}", e.getMessage());
                }
            });
            log.info("[SSE] Removed all emitters for stream: {}", streamId);
        }
    }

    public int getViewerCount(UUID streamId) {
        Set<ViewerEmitter> emitters = streamEmitters.get(streamId);
        return emitters != null ? emitters.size() : 0;
    }

    public boolean isViewerConnected(String viewerId) {
        return viewerEmitters.containsKey(viewerId);
    }

    public boolean isViewerConnectedToStream(UUID streamId, String viewerId) {
        UUID connectedStreamId = activeViewers.get(viewerId);
        return connectedStreamId != null && connectedStreamId.equals(streamId);
    }
    
    public void registerActiveViewer(UUID streamId, String viewerId) {
        activeViewers.put(viewerId, streamId);
    }
    
    public void unregisterActiveViewer(String viewerId) {
        activeViewers.remove(viewerId);
    }
    
    private void sendKeepaliveToAll() {
        int totalEmitters = viewerEmitters.size();
        if (totalEmitters == 0) {
            return;
        }
        
        List<String> failedViewers = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        for (ViewerEmitter viewerEmitter : viewerEmitters.values()) {
            try {
                viewerEmitter.emitter().send(SseEmitter.event()
                    .comment("keepalive")
                    .build());
            } catch (IOException | IllegalStateException e) {
                failedViewers.add(viewerEmitter.viewerId());
            }
        }
        
        failedViewers.forEach(this::removeEmitterInternal);
        
        long duration = System.currentTimeMillis() - startTime;
        if (failedViewers.size() > 0) {
            log.info("[SSE] Keepalive completed - Total: {}, Failed: {}, Duration: {}ms", 
                     totalEmitters, failedViewers.size(), duration);
        }
    }

    /**
     * Remove emitters that have exceeded TTL to prevent memory leaks
     */
    private void cleanupExpiredEmitters() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(sseProperties.getEmitterTtlHours());
        List<String> expiredViewers = new ArrayList<>();
        
        int totalBefore = viewerEmitters.size();
        
        for (ViewerEmitter viewerEmitter : viewerEmitters.values()) {
            if (viewerEmitter.createdAt().isBefore(cutoff)) {
                expiredViewers.add(viewerEmitter.viewerId());
            }
        }
        
        expiredViewers.forEach(viewerId -> {
            log.info("[SSE] Removing expired emitter - ViewerId: {}, Age: >{}h", 
                     viewerId, sseProperties.getEmitterTtlHours());
            removeEmitterInternal(viewerId);
        });
        
        if (expiredViewers.size() > 0) {
            log.info("[SSE] TTL Cleanup completed - Before: {}, Removed: {}, After: {}", 
                     totalBefore, expiredViewers.size(), viewerEmitters.size());
        }
    }

    private record ViewerEmitter(UUID streamId, String viewerId, SseEmitter emitter, Runnable onDisconnect, LocalDateTime createdAt) {}

    @PreDestroy
    public void shutdown() {
        log.info("[SSE] Shutting down keepalive scheduler");
        keepaliveScheduler.shutdownNow();
    }
}
