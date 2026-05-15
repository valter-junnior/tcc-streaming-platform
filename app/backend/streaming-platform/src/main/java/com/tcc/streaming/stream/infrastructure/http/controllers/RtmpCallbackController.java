package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.common.infrastructure.rtmp.RtmpGateway;
import com.tcc.streaming.stream.application.services.StreamService;
import com.tcc.streaming.stream.infrastructure.sse.controllers.StreamSseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/streams/callback")
@Tag(name = "RTMP Callbacks", description = "Callbacks dos servidores RTMP (Nginx-RTMP, SRS) para controle de streaming")
public class RtmpCallbackController {

    private static final Logger log = LoggerFactory.getLogger(RtmpCallbackController.class);
    private final StreamService streamService;
    private final StreamSseController sseController;
    private final RtmpGateway rtmpGateway;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public RtmpCallbackController(StreamService streamService, StreamSseController sseController,
                                  RtmpGateway rtmpGateway, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.streamService = streamService;
        this.sseController = sseController;
        this.rtmpGateway = rtmpGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * Extrai o nome da stream da requisição.
     * - Nginx-RTMP: envia POST com form-encoded body (name=stream_key).
     * - SRS 4.x: envia POST com JSON body ({"action":"on_publish","stream":"stream_key",...}).
     *   O campo name pode vir como query param se SRS substituiu [stream] na URL.
     */
    private String resolveStreamKey(HttpServletRequest request) {
        // Nginx-RTMP e query param (SRS com [stream] substituído)
        String name = request.getParameter("name");
        if (name != null && !name.isBlank() && !"[stream]".equals(name)) {
            return name;
        }
        // SRS: parse JSON body
        try {
            String body = request.getReader().lines().collect(Collectors.joining());
            if (body != null && !body.isBlank()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = objectMapper.readValue(body, Map.class);
                Object streamField = json.get("stream");
                if (streamField != null && !streamField.toString().isBlank()) {
                    return streamField.toString();
                }
                // Fallback: field "name" in JSON (some SRS builds use this)
                Object nameField = json.get("name");
                if (nameField != null && !nameField.toString().isBlank()) {
                    return nameField.toString();
                }
            }
        } catch (IOException e) {
            log.warn("[RtmpCallback] Could not parse request body: {}", e.getMessage());
        }
        return null;
    }

    @PostMapping("/publish")
    @Operation(
        summary = "Callback de autenticação e início de stream",
        description = "Chamado pelo servidor RTMP quando um streamer tenta iniciar uma transmissão. Valida a stream key e, se válida, marca a stream como LIVE. Compatível com Nginx-RTMP (form params) e SRS (JSON body)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream key válida - transmissão autorizada e iniciada"),
        @ApiResponse(responseCode = "403", description = "Stream key inválida - transmissão rejeitada")
    })
    public ResponseEntity<Map<String, Integer>> onPublish(HttpServletRequest request) {
        String name = resolveStreamKey(request);
        log.info("[RtmpCallback] ({}) Received on_publish for stream key: {}", rtmpGateway.getName(), name);

        if (name == null || name.isBlank()) {
            log.warn("[RtmpCallback] ({}) on_publish called with no stream key", rtmpGateway.getName());
            return ResponseEntity.badRequest().body(Map.of("code", 400));
        }

        UUID streamId = streamService.validateAndStartStream(name);
        if (streamId != null) {
            log.info("[RtmpCallback] ({}) Stream authorized and started - ID: {}, Key: {}", rtmpGateway.getName(), streamId, name);
            sseController.broadcastStreamStarted(streamId);
            log.debug("[RtmpCallback] SSE notification sent for stream: {}", streamId);
            // code:0 é exigido pelo SRS para aceitar a publicação; Nginx-RTMP ignora o corpo.
            return ResponseEntity.ok(Map.of("code", 0));
        } else {
            log.warn("[RtmpCallback] ({}) Stream rejected - Invalid key: {}", rtmpGateway.getName(), name);
            return ResponseEntity.status(403).body(Map.of("code", 403));
        }
    }

    @PostMapping("/publish_done")
    @Operation(
        summary = "Callback de finalização de stream",
        description = "Chamado pelo servidor RTMP quando uma transmissão é finalizada. Marca a stream como finalizada. Compatível com Nginx-RTMP e SRS."
    )
    public ResponseEntity<Map<String, Integer>> onPublishDone(HttpServletRequest request) {
        String name = resolveStreamKey(request);
        log.info("[RtmpCallback] ({}) Received on_publish_done for stream key: {}", rtmpGateway.getName(), name);

        if (name == null || name.isBlank()) {
            log.warn("[RtmpCallback] ({}) on_publish_done called with no stream key", rtmpGateway.getName());
            return ResponseEntity.ok(Map.of("code", 0));
        }

        UUID streamId = streamService.validateAndEndStream(name);
        if (streamId != null) {
            log.info("[RtmpCallback] ({}) Stream ended - ID: {}, Key: {}", rtmpGateway.getName(), streamId, name);
            sseController.broadcastStreamEnded(streamId);
            log.debug("[RtmpCallback] SSE notification sent for stream: {}", streamId);
            return ResponseEntity.ok(Map.of("code", 0));
        } else {
            log.warn("[RtmpCallback] ({}) Stream not found for key: {}", rtmpGateway.getName(), name);
            return ResponseEntity.ok(Map.of("code", 0));
        }
    }
}
