package com.tcc.streaming.stream.infrastructure.http.presenters;

import com.tcc.streaming.stream.core.dtos.stream.StreamStatusDto;
import com.tcc.streaming.stream.core.entities.StreamStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Status resumido de uma stream")
public record StreamStatusPresenter(
    @Schema(description = "ID único da stream", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Status atual da stream", example = "LIVE")
    StreamStatus status,
    
    @Schema(description = "Número atual de viewers assistindo", example = "42")
    Integer currentViewers,
    
    @Schema(description = "Pico de viewers simultâneos", example = "105")
    Integer viewersPeak,
    
    @Schema(description = "URL para assistir a stream", example = "http://localhost:3001/watch/550e8400-e29b-41d4-a716-446655440000")
    String watchUrl
) {
    public static StreamStatusPresenter from(StreamStatusDto dto) {
        return new StreamStatusPresenter(
            dto.id(),
            dto.status(),
            dto.currentViewers(),
            dto.viewersPeak(),
            dto.watchUrl()
        );
    }
}
