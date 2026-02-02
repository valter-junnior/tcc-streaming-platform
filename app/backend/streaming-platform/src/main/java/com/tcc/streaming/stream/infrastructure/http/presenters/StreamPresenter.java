package com.tcc.streaming.stream.infrastructure.http.presenters;

import com.tcc.streaming.stream.core.dtos.stream.StreamDto;
import com.tcc.streaming.stream.core.entities.StreamStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Detalhes completos de uma stream")
public record StreamPresenter(
    @Schema(description = "ID único da stream", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Título da stream", example = "Minha Live de Games")
    String title,
    
    @Schema(description = "Descrição da stream", example = "Jogando Minecraft com os amigos!")
    String description,
    
    @Schema(description = "Chave única para transmissão RTMP", example = "a1b2c3d4e5f6g7h8")
    String streamKey,
    
    @Schema(description = "Status atual da stream", example = "LIVE")
    StreamStatus status,
    
    @Schema(description = "Data e hora de criação da stream", example = "2026-02-02T10:30:00")
    LocalDateTime createdAt,
    
    @Schema(description = "Data e hora de início da transmissão", example = "2026-02-02T10:35:00")
    LocalDateTime startedAt,
    
    @Schema(description = "Data e hora de fim da transmissão", example = "2026-02-02T12:00:00")
    LocalDateTime endedAt,
    
    @Schema(description = "Número atual de viewers assistindo", example = "127")
    Integer currentViewers,
    
    @Schema(description = "Pico de viewers simultâneos", example = "543")
    Integer viewersPeak,
    
    @Schema(description = "URL RTMP completa para transmissão", example = "rtmp://localhost:1935/live/a1b2c3d4e5f6g7h8")
    String rtmpUrl,
    
    @Schema(description = "URL para assistir a stream no navegador", example = "http://localhost:3000/watch/550e8400-e29b-41d4-a716-446655440000")
    String watchUrl
) {
    public static StreamPresenter from(StreamDto dto) {
        return new StreamPresenter(
            dto.id(),
            dto.title(),
            dto.description(),
            dto.streamKey(),
            dto.status(),
            dto.createdAt(),
            dto.startedAt(),
            dto.endedAt(),
            dto.currentViewers(),
            dto.viewersPeak(),
            dto.rtmpUrl(),
            dto.watchUrl()
        );
    }
}
