package com.tcc.streaming.stream.infrastructure.http.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStreamRequest(
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
    String title,
    
    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    String description
) {}
