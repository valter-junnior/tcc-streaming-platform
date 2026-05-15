package com.tcc.streaming.common.core.dtos;

/**
 * DTO para configurações gerais da aplicação
 */
public record ConfigDto(
    String timezone,
    String version,
    String environment,
    Long serverTimestamp,
    String transcoderType
) {
    
    /**
     * Factory method para criar configuração com timestamp atual
     */
    public static ConfigDto create(String timezone, String version, String environment, String transcoderType) {
        return new ConfigDto(
            timezone,
            version, 
            environment,
            System.currentTimeMillis(),
            transcoderType
        );
    }
}