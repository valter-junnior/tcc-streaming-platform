package com.tcc.streaming.common.infrastructure.http.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@Tag(name = "Root", description = "Informações gerais da API")
public class RootController {

    @GetMapping
    @Operation(
        summary = "Informações da API",
        description = "Retorna informações básicas sobre a API, versão, status e endpoints disponíveis"
    )
    @ApiResponse(responseCode = "200", description = "Informações retornadas com sucesso")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Streaming Platform API");
        response.put("version", "1.0.0-SNAPSHOT");
        response.put("status", "online");
        response.put("timestamp", LocalDateTime.now());
        response.put("endpoints", Map.of(
            "streams", "/api/streams",
            "health", "/actuator/health",
            "metrics", "/actuator/metrics",
            "websocket", "/ws"
        ));
        
        return ResponseEntity.ok(response);
    }
}
