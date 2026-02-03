package com.tcc.streaming.stream.infrastructure.websocket.listeners;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import com.tcc.streaming.stream.application.services.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener para eventos de WebSocket (conexão/desconexão)
 * Garante que viewers sejam decrementados mesmo se não enviarem viewer_left explicitamente
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final StreamService streamService;
    private final EventPublisher eventPublisher;
    
    // Mapeia sessionId -> (streamId, viewerId)
    private final Map<String, ViewerSession> activeSessions = new ConcurrentHashMap<>();

    public WebSocketEventListener(StreamService streamService, EventPublisher eventPublisher) {
        this.streamService = streamService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registra uma sessão de viewer quando ele entra na stream
     */
    public void registerViewerSession(String sessionId, UUID streamId, String viewerId) {
        activeSessions.put(sessionId, new ViewerSession(streamId, viewerId));
        log.debug("[WebSocketListener] Registered viewer session - SessionId: {}, StreamId: {}, ViewerId: {}", 
                  sessionId, streamId, viewerId);
    }

    /**
     * Remove uma sessão de viewer quando ele sai explicitamente
     */
    public void unregisterViewerSession(String sessionId) {
        activeSessions.remove(sessionId);
        log.debug("[WebSocketListener] Unregistered viewer session - SessionId: {}", sessionId);
    }

    /**
     * Detecta desconexão de WebSocket (browser fechou, crash, timeout, etc)
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.debug("[WebSocketListener] WebSocket disconnected - SessionId: {}", sessionId);
        
        // Verificar se essa sessão tinha um viewer ativo
        ViewerSession viewerSession = activeSessions.remove(sessionId);
        
        if (viewerSession != null) {
            log.info("[WebSocketListener] Auto-decrementing viewer after disconnect - StreamId: {}, ViewerId: {}", 
                     viewerSession.streamId(), viewerSession.viewerId());
            
            try {
                // Decrementar viewers automaticamente
                streamService.decrementViewers(viewerSession.streamId());
                
                // Publicar evento viewer_left
                eventPublisher.publishViewerLeft(viewerSession.streamId(), viewerSession.viewerId());
            } catch (Exception e) {
                log.error("[WebSocketListener] Error auto-decrementing viewer: {}", e.getMessage(), e);
            }
        }
    }

    private record ViewerSession(UUID streamId, String viewerId) {}
}
