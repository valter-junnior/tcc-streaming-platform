package com.tcc.streaming.stream.infrastructure.sse.controllers;

import com.tcc.streaming.stream.application.services.StreamService;
import com.tcc.streaming.stream.infrastructure.sse.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Controller para conexões SSE (Server-Sent Events)
 * APENAS para notificações em tempo real - NÃO manipula dados
 * 
 * Join/Leave de viewers é feito via REST em ViewerController
 * SSE detecta desconexão e chama leave automaticamente
 */
@RestController
@RequestMapping("/api/sse")
public class StreamSseController {

    private static final Logger log = LoggerFactory.getLogger(StreamSseController.class);
    
    private final SseEmitterManager emitterManager;
    private final RestTemplate restTemplate;
    private final StreamService streamService;

    public StreamSseController(SseEmitterManager emitterManager, StreamService streamService) {
        this.emitterManager = emitterManager;
        this.restTemplate = new RestTemplate();
        this.streamService = streamService;
    }

    /**
     * Conecta ao SSE para receber notificações
     * NÃO incrementa viewers - isso é feito via POST /join
     */
    @GetMapping(value = "/stream/{streamId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable UUID streamId, 
                                @RequestParam String viewerId,
                                @RequestParam(defaultValue = "true") boolean countAsViewer) {
        
        log.info("[SSE] Connecting for notifications - StreamId: {}, ViewerId: {}, CountAsViewer: {}", 
                 streamId, viewerId, countAsViewer);
        
        // ✅ CRÍTICO: Validar se stream existe ANTES de criar SseEmitter
        // Caso contrário, GlobalExceptionHandler tenta retornar ErrorResponse com Content-Type text/event-stream
        try {
            streamService.execute(streamId);
        } catch (Exception e) {
            log.error("[SSE] Stream validation failed - StreamId: {}, Error: {}", streamId, e.getMessage());
            throw e; // Re-throw para que GlobalExceptionHandler trate ANTES de criar o emitter
        }
        
        // Criar emitter APENAS para notificações
        // Não faz nenhuma operação de banco de dados aqui
        SseEmitter emitter = emitterManager.createEmitter(streamId, viewerId, () -> {
            // Callback de desconexão - executado quando browser fecha ou navegação SPA
            log.warn("[SSE] DISCONNECT CALLBACK EXECUTING - StreamId: {}, ViewerId: {}, CountAsViewer: {}", 
                     streamId, viewerId, countAsViewer);
            
            // Quando o SSE desconectar, chamar o endpoint /leave automaticamente
            if (countAsViewer) {
                try {
                    String url = String.format("http://localhost:8080/api/streams/%s/leave?viewerId=%s&countAsViewer=true", 
                                              streamId, viewerId);
                    
                    log.info("[SSE] Calling leave endpoint on disconnect - URL: {}", url);
                    restTemplate.postForEntity(url, null, Void.class);
                    
                } catch (Exception e) {
                    log.error("[SSE] Error calling leave endpoint: {}", e.getMessage(), e);
                }
            }
        });
        
        log.info("[SSE] Connection established - StreamId: {}, ViewerId: {}", streamId, viewerId);
        return emitter;
    }

    /**
     * Broadcast atualização de status da stream (chamado pelo NginxCallbackController)
     */
    public void broadcastStreamStarted(UUID streamId) {
        log.info("[SSE] Broadcasting stream started - StreamId: {}", streamId);
        emitterManager.broadcastToStream(streamId, "stream_status", 
            new StreamStatusMessage("STREAM_STARTED", "LIVE"));
    }

    /**
     * Broadcast stream encerrada
     */
    public void broadcastStreamEnded(UUID streamId) {
        log.info("[SSE] Broadcasting stream ended - StreamId: {}", streamId);
        emitterManager.broadcastToStream(streamId, "stream_status", 
            new StreamStatusMessage("STREAM_ENDED", "ENDED"));
        
        // Remover todos os emitters após 5 segundos (dar tempo para receber a mensagem)
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                emitterManager.removeAllEmittersForStream(streamId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Broadcast atualização de viewers (chamado pelo ViewerController)
     */
    public void broadcastViewersUpdate(UUID streamId, int currentViewers, int viewersPeak) {
        log.debug("[SSE] Broadcasting viewers update - StreamId: {}, Current: {}, Peak: {}", 
                  streamId, currentViewers, viewersPeak);
        emitterManager.broadcastToStream(streamId, "viewers_update", 
            new ViewersUpdateMessage(currentViewers, viewersPeak));
    }

    // DTOs para mensagens SSE
    public record StreamStatusMessage(String type, String status) {}
    public record ViewersUpdateMessage(int currentViewers, int viewersPeak) {}
}
