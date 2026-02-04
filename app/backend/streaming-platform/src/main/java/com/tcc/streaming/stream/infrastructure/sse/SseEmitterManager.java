package com.tcc.streaming.stream.infrastructure.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Gerenciador de conexões SSE (Server-Sent Events)
 * Responsável por:
 * - Manter mapeamento de emitters por stream
 * - Enviar eventos para viewers específicos
 * - Detectar desconexões automaticamente via timeout/completion/error
 * - Enviar keepalive periódico para manter conexões vivas
 */
@Component
public class SseEmitterManager {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterManager.class);
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutos
    private static final Long KEEPALIVE_INTERVAL = 30 * 1000L; // 30 segundos
    
    private final ScheduledExecutorService keepaliveScheduler = Executors.newScheduledThreadPool(1);
    
    private final ObjectMapper objectMapper;
    
    // Mapeia streamId -> Set de ViewerEmitter
    private final Map<UUID, Set<ViewerEmitter>> streamEmitters = new ConcurrentHashMap<>();
    
    // Mapeia viewerId -> ViewerEmitter para remoção rápida
    private final Map<String, ViewerEmitter> viewerEmitters = new ConcurrentHashMap<>();
    
    // Mapeia viewerId -> streamId para detectar reconexões antes do SSE conectar
    // Usado para evitar incremento duplo no contador quando user faz refresh
    private final Map<String, UUID> activeViewers = new ConcurrentHashMap<>();

    public SseEmitterManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        // Iniciar tarefa de keepalive periódico
        keepaliveScheduler.scheduleAtFixedRate(() -> {
            try {
                sendKeepaliveToAll();
            } catch (Exception e) {
                log.error("[SSE] Error in keepalive task", e);
            }
        }, KEEPALIVE_INTERVAL, KEEPALIVE_INTERVAL, TimeUnit.MILLISECONDS);
        
        log.info("[SSE] Keepalive scheduler started - Interval: {}s", KEEPALIVE_INTERVAL / 1000);
    }

    /**
     * Cria e registra um novo SSE emitter para um viewer
     * Retorna callback para ser executado quando o viewer desconectar
     */
    public SseEmitter createEmitter(UUID streamId, String viewerId, Runnable onDisconnect) {
        // Se já existe um emitter para esse viewerId, remover primeiro
        if (viewerEmitters.containsKey(viewerId)) {
            log.info("[SSE] Removing existing emitter before creating new one - ViewerId: {}", viewerId);
            removeEmitterInternal(viewerId);
        }
        
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        ViewerEmitter viewerEmitter = new ViewerEmitter(streamId, viewerId, emitter, onDisconnect);
        
        // Registrar emitter
        streamEmitters.computeIfAbsent(streamId, k -> new CopyOnWriteArraySet<>()).add(viewerEmitter);
        viewerEmitters.put(viewerId, viewerEmitter);
        
        // Enviar keepalive inicial (comment) para confirmar conexão
        try {
            emitter.send(SseEmitter.event()
                .comment("Connected to stream " + streamId)
                .build());
        } catch (IOException e) {
            log.warn("[SSE] Failed to send initial keepalive - ViewerId: {}", viewerId);
        }
        
        log.info("[SSE] Registered emitter - StreamId: {}, ViewerId: {}, Total viewers: {}", 
                 streamId, viewerId, streamEmitters.get(streamId).size());
        
        // Flag para evitar múltiplas chamadas ao callback
        final boolean[] callbackExecuted = {false};
        
        Runnable safeDisconnect = () -> {
            synchronized (callbackExecuted) {
                if (!callbackExecuted[0]) {
                    callbackExecuted[0] = true;
                    removeEmitterInternal(viewerId);
                    onDisconnect.run();
                }
            }
        };
        
        // Configurar callbacks de lifecycle
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

    /**
     * Remove um emitter específico (método público)
     */
    public void removeEmitter(String viewerId) {
        removeEmitterInternal(viewerId);
    }
    
    /**
     * Remove um emitter específico (método interno)
     */
    private void removeEmitterInternal(String viewerId) {
        ViewerEmitter viewerEmitter = viewerEmitters.remove(viewerId);
        if (viewerEmitter != null) {
            Set<ViewerEmitter> emitters = streamEmitters.get(viewerEmitter.streamId());
            if (emitters != null) {
                emitters.remove(viewerEmitter);
                log.debug("[SSE] Removed emitter - StreamId: {}, ViewerId: {}, Remaining viewers: {}", 
                          viewerEmitter.streamId(), viewerId, emitters.size());
                
                // Limpar set se vazio
                if (emitters.isEmpty()) {
                    streamEmitters.remove(viewerEmitter.streamId());
                }
            }
        }
    }

    /**
     * Envia evento para todos os viewers de uma stream
     */
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
        
        // Remover emitters que falharam
        failedViewers.forEach(this::removeEmitterInternal);
    }

    /**
     * Remove todos os emitters de uma stream
     */
    public void removeAllEmittersForStream(UUID streamId) {
        Set<ViewerEmitter> emitters = streamEmitters.remove(streamId);
        if (emitters != null) {
            emitters.forEach(ve -> {
                viewerEmitters.remove(ve.viewerId());
                try {
                    ve.emitter().complete();
                } catch (Exception e) {
                    log.debug("[SSE] Error completing emitter: {}", e.getMessage());
                }
            });
            log.info("[SSE] Removed all emitters for stream: {}", streamId);
        }
    }

    /**
     * Retorna número de viewers conectados para uma stream
     */
    public int getViewerCount(UUID streamId) {
        Set<ViewerEmitter> emitters = streamEmitters.get(streamId);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * Verifica se um viewer está conectado (globalmente)
     */
    public boolean isViewerConnected(String viewerId) {
        return viewerEmitters.containsKey(viewerId);
    }

    /**
     * Verifica se um viewer está conectado em uma stream específica
     */
    public boolean isViewerConnectedToStream(UUID streamId, String viewerId) {
        UUID connectedStreamId = activeViewers.get(viewerId);
        return connectedStreamId != null && connectedStreamId.equals(streamId);
    }
    
    /**
     * Registra um viewer como ativo em uma stream
     * Chamado pelo ViewerController após incrementar contador
     */
    public void registerActiveViewer(UUID streamId, String viewerId) {
        activeViewers.put(viewerId, streamId);
        log.debug("[SSE] Registered active viewer - StreamId: {}, ViewerId: {}", streamId, viewerId);
    }
    
    /**
     * Remove um viewer da lista de ativos
     * Chamado pelo ViewerController após decrementar contador
     */
    public void unregisterActiveViewer(String viewerId) {
        UUID streamId = activeViewers.remove(viewerId);
        if (streamId != null) {
            log.debug("[SSE] Unregistered active viewer - StreamId: {}, ViewerId: {}", streamId, viewerId);
        }
    }
    
    /**
     * Envia keepalive (comment) para todos os emitters conectados
     * Previne timeout de proxies/firewalls que fecham conexões idle
     * Suporta até 5k usuários simultâneos com overhead mínimo
     */
    private void sendKeepaliveToAll() {
        int totalEmitters = viewerEmitters.size();
        if (totalEmitters == 0) {
            return;
        }
        
        log.debug("[SSE] Sending keepalive to {} emitters", totalEmitters);
        
        List<String> failedViewers = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        for (ViewerEmitter viewerEmitter : viewerEmitters.values()) {
            try {
                // Enviar comment (não gera evento no EventSource do cliente)
                viewerEmitter.emitter().send(SseEmitter.event()
                    .comment("keepalive")
                    .build());
            } catch (IOException | IllegalStateException e) {
                log.debug("[SSE] Keepalive failed for viewer: {} - {}", 
                         viewerEmitter.viewerId(), e.getMessage());
                failedViewers.add(viewerEmitter.viewerId());
            }
        }
        
        // Remover emitters que falharam (desconectados)
        failedViewers.forEach(this::removeEmitterInternal);
        
        long duration = System.currentTimeMillis() - startTime;
        if (failedViewers.size() > 0) {
            log.info("[SSE] Keepalive completed - Total: {}, Failed: {}, Duration: {}ms", 
                     totalEmitters, failedViewers.size(), duration);
        }
    }

    private record ViewerEmitter(UUID streamId, String viewerId, SseEmitter emitter, Runnable onDisconnect) {}
}
