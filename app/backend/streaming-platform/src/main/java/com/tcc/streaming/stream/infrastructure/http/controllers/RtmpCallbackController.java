package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.common.infrastructure.rtmp.RtmpGateway;
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
public class RtmpCallbackController {

    private static final Logger log = LoggerFactory.getLogger(RtmpCallbackController.class);
    private final StreamService streamService;
    private final StreamSseController sseController;
    private final RtmpGateway rtmpGateway;

    public RtmpCallbackController(StreamService streamService, StreamSseController sseController, RtmpGateway rtmpGateway) {
        this.streamService = streamService;
        this.sseController = sseController;
        this.rtmpGateway = rtmpGateway;
    }

    @PostMapping("/publish")
    @Operation(
        summary = "Callback de autenticação e início de stream",
        description = "Chamado pelo servidor RTMP quando um streamer tenta iniciar uma transmissão. Valida a stream key e, se válida, marca a stream como LIVE. Indica qual servidor está ativo."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream key válida - transmissão autorizada e iniciada"),
        @ApiResponse(responseCode = "403", description = "Stream key inválida - transmissão rejeitada")
    })
    public ResponseEntity<Void> onPublish(
        @Parameter(description = "Stream key do streamer")
        @RequestParam String name) {
        log.info("[RtmpCallback] ({}) Received on_publish for stream key: {}", rtmpGateway.getName(), name);
        UUID streamId = streamService.validateAndStartStream(name);
        if (streamId != null) {
            log.info("[RtmpCallback] ({}) Stream authorized and started - ID: {}, Key: {}", rtmpGateway.getName(), streamId, name);
            sseController.broadcastStreamStarted(streamId);
            log.debug("[RtmpCallback] SSE notification sent for stream: {}", streamId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("[RtmpCallback] ({}) Stream rejected - Invalid key: {}", rtmpGateway.getName(), name);
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/publish_done")
    @Operation(
        summary = "Callback de finalização de stream",
        description = "Chamado pelo servidor RTMP quando uma transmissão é finalizada. Marca a stream como finalizada. Indica qual servidor está ativo."
    )
    public ResponseEntity<Void> onPublishDone(@RequestParam String name) {
        log.info("[RtmpCallback] ({}) Received on_publish_done for stream key: {}", rtmpGateway.getName(), name);
        UUID streamId = streamService.validateAndEndStream(name);
        if (streamId != null) {
            log.info("[RtmpCallback] ({}) Stream ended - ID: {}, Key: {}", rtmpGateway.getName(), streamId, name);
            sseController.broadcastStreamEnded(streamId);
            log.debug("[RtmpCallback] SSE notification sent for stream: {}", streamId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("[RtmpCallback] ({}) Stream not found for key: {}", rtmpGateway.getName(), name);
            return ResponseEntity.status(404).build();
        }
    }
}
