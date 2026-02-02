package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.stream.core.dtos.stream.CreateStreamDto;
import com.tcc.streaming.stream.core.usecases.CreateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.DeleteStreamUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamStatusUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ValidateStreamKeyUseCase;
import com.tcc.streaming.stream.infrastructure.http.presenters.StreamPresenter;
import com.tcc.streaming.stream.infrastructure.http.presenters.StreamStatusPresenter;
import com.tcc.streaming.stream.infrastructure.http.requests.CreateStreamRequest;
import com.tcc.streaming.stream.infrastructure.http.requests.ValidateStreamKeyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/streams")
@Tag(name = "Streams", description = "Gerenciamento de streams de vídeo ao vivo")
public class StreamController {

    private final CreateStreamUseCase createStreamUseCase;
    private final GetStreamUseCase getStreamUseCase;
    private final GetStreamStatusUseCase getStreamStatusUseCase;
    private final DeleteStreamUseCase deleteStreamUseCase;
    private final ValidateStreamKeyUseCase validateStreamKeyUseCase;

    public StreamController(
        CreateStreamUseCase createStreamUseCase,
        GetStreamUseCase getStreamUseCase,
        GetStreamStatusUseCase getStreamStatusUseCase,
        DeleteStreamUseCase deleteStreamUseCase,
        ValidateStreamKeyUseCase validateStreamKeyUseCase
    ) {
        this.createStreamUseCase = createStreamUseCase;
        this.getStreamUseCase = getStreamUseCase;
        this.getStreamStatusUseCase = getStreamStatusUseCase;
        this.deleteStreamUseCase = deleteStreamUseCase;
        this.validateStreamKeyUseCase = validateStreamKeyUseCase;
    }

    @PostMapping
    @Operation(
        summary = "Criar nova stream",
        description = "Cria uma nova stream e retorna as credenciais RTMP para transmissão"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Stream criada com sucesso",
            content = @Content(schema = @Schema(implementation = StreamPresenter.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<StreamPresenter> createStream(
        @Parameter(description = "Dados da stream a ser criada")
        @Valid @RequestBody CreateStreamRequest request) {
        var dto = new CreateStreamDto(request.title(), request.description());
        var result = createStreamUseCase.execute(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(StreamPresenter.from(result));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar stream por ID",
        description = "Retorna os detalhes completos de uma stream específica"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream encontrada",
            content = @Content(schema = @Schema(implementation = StreamPresenter.class))),
        @ApiResponse(responseCode = "404", description = "Stream não encontrada")
    })
    public ResponseEntity<StreamPresenter> getStream(
        @Parameter(description = "ID único da stream (UUID)")
        @PathVariable UUID id) {
        var result = getStreamUseCase.execute(id);
        return ResponseEntity.ok(StreamPresenter.from(result));
    }

    @GetMapping("/{id}/status")
    @Operation(
        summary = "Buscar status da stream",
        description = "Retorna o status atual da stream e contador de viewers (dados cacheados)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status obtido com sucesso",
            content = @Content(schema = @Schema(implementation = StreamStatusPresenter.class))),
        @ApiResponse(responseCode = "404", description = "Stream não encontrada")
    })
    public ResponseEntity<StreamStatusPresenter> getStreamStatus(
        @Parameter(description = "ID único da stream (UUID)")
        @PathVariable UUID id) {
        var result = getStreamStatusUseCase.getStatus(id);
        return ResponseEntity.ok(StreamStatusPresenter.from(result));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deletar stream",
        description = "Finaliza uma stream, alterando seu status para ENDED"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Stream deletada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Stream não encontrada")
    })
    public ResponseEntity<Void> deleteStream(
        @Parameter(description = "ID único da stream (UUID)")
        @PathVariable UUID id) {
        deleteStreamUseCase.deleteStream(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate")
    @Operation(
        summary = "Validar stream key",
        description = "Valida se uma stream key existe e está ativa no sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validação realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Stream key inválida")
    })
    public ResponseEntity<Boolean> validateStreamKey(
        @Parameter(description = "Stream key a ser validada")
        @Valid @RequestBody ValidateStreamKeyRequest request) {
        boolean valid = validateStreamKeyUseCase.execute(request.streamKey());
        return ResponseEntity.ok(valid);
    }
}
