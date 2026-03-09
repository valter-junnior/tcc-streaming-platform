package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.stream.application.services.StreamService;
import com.tcc.streaming.stream.infrastructure.sse.controllers.StreamSseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/streams/callback")
@Tag(name = "RTMP Callbacks", description = "Callbacks dos servidores RTMP (Nginx-RTMP, SRS) para controle de streaming")
public class NginxCallbackController {

    private static final Logger log = LoggerFactory.getLogger(NginxCallbackController.class);
    private final StreamService streamService;
    private final StreamSseController sseController;

    public NginxCallbackController(StreamService streamService, StreamSseController sseController) {
        this.streamService = streamService;
        this.sseController = sseController;
    }

    @PostMapping("/publish")
    @Operation(
        summary = "Callback de autenticação e início de stream",
        description = "Chamado pelo servidor RTMP quando um streamer tenta iniciar uma transmissão. Valida a stream key e, se válida, marca a stream como LIVE."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream key válida - transmissão autorizada e iniciada"),
        @ApiResponse(responseCode = "403", description = "Stream key inválida - transmissão rejeitada")
    })
    public ResponseEntity<Void> onPublish(
        @Parameter(description = "Stream key do streamer")
        @RequestParam String name) {
        log.info("[NginxCallback] Received on_publish for stream key: {}", name);
        
        // Operação atômica: validar e iniciar stream em uma única transação
        UUID streamId = streamService.validateAndStartStream(name);
        
        if (streamId != null) {
            // Stream key válida e stream iniciada com sucesso
            log.info("[NginxCallback] Stream authorized and started - ID: {}, Key: {}", streamId, name);
            
            // Notificar via SSE que a stream iniciou
            sseController.broadcastStreamStarted(streamId);
            log.debug("[NginxCallback] SSE notification sent for stream: {}", streamId);
            
            return ResponseEntity.ok().build();
        } else {
            log.warn("[NginxCallback] Stream rejected - Invalid key: {}", name);
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/publish_done")
    @Operation(
        summary = "Callback de fim de transmissão",
        description = "Chamado pelo servidor RTMP quando o streamer para/desconecta da transmissão. Marca a stream como ENDED e executa limpeza."
    )
    @ApiResponse(responseCode = "200", description = "Stream finalizada com sucesso")
    public ResponseEntity<Void> onPublishDone(
        @Parameter(description = "Stream key da transmissão")
        @RequestParam String name) {
        log.info("[NginxCallback] Received on_publish_done for stream key: {}", name);
        
        UUID streamId = streamService.endStream(name);
        log.info("[NginxCallback] Stream ended - ID: {}, Key: {}", streamId, name);

        // Notificar via SSE que a stream terminou
        sseController.broadcastStreamEnded(streamId);
        log.debug("[NginxCallback] SSE notification sent for stream: {}", streamId);

        return ResponseEntity.ok().build();
    }
}
