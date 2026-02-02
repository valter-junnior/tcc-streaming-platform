package com.tcc.streaming.stream.infrastructure.http.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidateStreamKeyRequest(
    @NotBlank(message = "Stream key é obrigatória")
    @Size(min = 16, max = 16, message = "Stream key deve ter 16 caracteres")
    String streamKey
) {}
