package com.tcc.streaming.stream.infrastructure.http.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para validação de stream key")
public record ValidateStreamKeyRequest(
    @Schema(description = "Chave única da stream (16 caracteres)", example = "a1b2c3d4e5f6g7h8", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Stream key é obrigatória")
    @Size(min = 16, max = 16, message = "Stream key deve ter 16 caracteres")
    String streamKey
) {}
