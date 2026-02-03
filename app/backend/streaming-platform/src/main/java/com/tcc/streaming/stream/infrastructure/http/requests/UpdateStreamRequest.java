package com.tcc.streaming.stream.infrastructure.http.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualização de uma stream")
public record UpdateStreamRequest(
    @Schema(description = "Título da stream", example = "Nova Live de Games", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
    String title,
    
    @Schema(description = "Descrição da stream", example = "Jogando Valorant ranked")
    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    String description,
    
    @Schema(description = "ID do usuário dono da stream", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Owner ID é obrigatório")
    @Size(max = 36, message = "Owner ID deve ter no máximo 36 caracteres")
    String ownerId
) {}
