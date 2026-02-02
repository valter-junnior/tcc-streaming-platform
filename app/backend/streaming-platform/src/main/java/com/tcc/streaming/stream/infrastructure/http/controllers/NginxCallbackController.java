package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.stream.application.services.StreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streams/callback")
@Tag(name = "Nginx Callbacks", description = "Callbacks do servidor Nginx-RTMP para controle de streaming")
public class NginxCallbackController {

    private final StreamService streamService;

    public NginxCallbackController(StreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping("/publish")
    @Operation(
        summary = "Callback de publicação",
        description = "Chamado pelo Nginx-RTMP quando um streamer tenta iniciar uma transmissão. Valida a stream key."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream key válida - transmissão autorizada"),
        @ApiResponse(responseCode = "403", description = "Stream key inválida - transmissão rejeitada")
    })
    public ResponseEntity<Void> onPublish(
        @Parameter(description = "Stream key do streamer")
        @RequestParam String name) {
        boolean valid = streamService.execute(name);
        return valid ? ResponseEntity.ok().build() : ResponseEntity.status(403).build();
    }

    @PostMapping("/publish_done")
    @Operation(
        summary = "Callback de início de transmissão",
        description = "Chamado pelo Nginx-RTMP quando a transmissão efetivamente inicia. Atualiza status da stream para LIVE."
    )
    @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso")
    public ResponseEntity<Void> onPublishDone(
        @Parameter(description = "Stream key da transmissão")
        @RequestParam String name) {
        streamService.startStream(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/done")
    @Operation(
        summary = "Callback de fim de transmissão",
        description = "Chamado pelo Nginx-RTMP quando a transmissão é encerrada. Atualiza status da stream para ENDED."
    )
    @ApiResponse(responseCode = "200", description = "Stream finalizada com sucesso")
    public ResponseEntity<Void> onDone(
        @Parameter(description = "Stream key da transmissão")
        @RequestParam String name) {
        try {
            streamService.endStream(name);
        } catch (Exception e) {
            // Log error but return 200 to Nginx
        }
        return ResponseEntity.ok().build();
    }
}
