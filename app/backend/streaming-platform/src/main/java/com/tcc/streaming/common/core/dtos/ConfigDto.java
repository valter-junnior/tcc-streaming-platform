package com.tcc.streaming.common.core.dtos;

/**
 * DTO para configurações gerais da aplicação
 */
public record ConfigDto(
    String timezone,
    String version,
    String environment,
    Long serverTimestamp
) {
    
    /**
     * Factory method para criar configuração com timestamp atual
     */
    public static ConfigDto create(String timezone, String version, String environment) {
        return new ConfigDto(
            timezone,
            version, 
            environment,
            System.currentTimeMillis()
        );
    }
}