package com.tcc.streaming.common.infrastructure.http.controllers;

import com.tcc.streaming.common.core.dtos.ConfigDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;

/**
 * Controller para configurações gerais da aplicação
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "Config", description = "Configurações gerais da aplicação")
public class ConfigController {

    @Value("${spring.application.name:streaming-platform}")
    private String applicationName;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Operation(summary = "Obter configurações gerais", 
               description = "Retorna configurações globais incluindo timezone, versão e timestamp do servidor")
    @GetMapping
    public ResponseEntity<ConfigDto> getConfig() {
        // Obter timezone do sistema/JVM
        String systemTimezone = ZoneId.systemDefault().getId();
        
        ConfigDto config = ConfigDto.create(
            systemTimezone,
            "1.0.0-SNAPSHOT", 
            activeProfile
        );
        
        return ResponseEntity.ok(config);
    }
}