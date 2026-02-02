package com.tcc.streaming.stream.infrastructure.http.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criação de uma nova stream")
public record CreateStreamRequest(
    @Schema(description = "Título da stream", example = "Minha Live de Games", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
    String title,
    
    @Schema(description = "Descrição da stream", example = "Jogando Minecraft com os amigos!")
    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    String description
) {}
