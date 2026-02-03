package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.stream.application.services.StreamService;
import com.tcc.streaming.stream.core.dtos.stream.CreateStreamDto;
import com.tcc.streaming.stream.core.usecases.CreateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.DeleteStreamUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamStatusUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ListLiveStreamsUseCase;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/streams")
@Tag(name = "Streams", description = "Gerenciamento de streams de vídeo ao vivo")
public class StreamController {

    private static final Logger log = LoggerFactory.getLogger(StreamController.class);
    private final CreateStreamUseCase createStreamUseCase;
    private final GetStreamUseCase getStreamUseCase;
    private final GetStreamStatusUseCase getStreamStatusUseCase;
    private final DeleteStreamUseCase deleteStreamUseCase;
    private final ValidateStreamKeyUseCase validateStreamKeyUseCase;
    private final ListLiveStreamsUseCase listLiveStreamsUseCase;
    private final StreamService streamService;

    public StreamController(
        CreateStreamUseCase createStreamUseCase,
        GetStreamUseCase getStreamUseCase,
        GetStreamStatusUseCase getStreamStatusUseCase,
        DeleteStreamUseCase deleteStreamUseCase,
        ValidateStreamKeyUseCase validateStreamKeyUseCase,
        ListLiveStreamsUseCase listLiveStreamsUseCase,
        StreamService streamService
    ) {
        this.createStreamUseCase = createStreamUseCase;
        this.getStreamUseCase = getStreamUseCase;
        this.getStreamStatusUseCase = getStreamStatusUseCase;
        this.deleteStreamUseCase = deleteStreamUseCase;
        this.validateStreamKeyUseCase = validateStreamKeyUseCase;
        this.listLiveStreamsUseCase = listLiveStreamsUseCase;
        this.streamService = streamService;
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
        log.info("[Stream] Creating new stream - Title: {}", request.title());
        var dto = new CreateStreamDto(request.title(), request.description());
        var result = createStreamUseCase.execute(dto);
        log.info("[Stream] Stream created successfully - ID: {}, Key: {}", result.id(), result.streamKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(StreamPresenter.from(result));
    }

    @GetMapping("/live")
    @Operation(
        summary = "Listar streams ao vivo",
        description = "Retorna todas as streams com status LIVE (dados cacheados)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de streams LIVE retornada com sucesso",
            content = @Content(schema = @Schema(implementation = StreamPresenter.class)))
    })
    public ResponseEntity<List<StreamPresenter>> getLiveStreams() {
        log.debug("[Stream] Fetching all LIVE streams");
        var result = listLiveStreamsUseCase.execute();
        var presenters = result.stream()
            .map(StreamPresenter::from)
            .collect(java.util.stream.Collectors.toList());
        log.debug("[Stream] Found {} LIVE streams", presenters.size());
        return ResponseEntity.ok(presenters);
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
        log.debug("[Stream] Getting stream by ID: {}", id);
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
        log.debug("[Stream] Getting stream status - ID: {}", id);
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
        log.info("[Stream] Deleting stream - ID: {}", id);
        deleteStreamUseCase.deleteStream(id);
        log.info("[Stream] Stream deleted successfully - ID: {}", id);
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
        log.debug("[Stream] Validating stream key");
        boolean valid = validateStreamKeyUseCase.execute(request.streamKey());
        log.debug("[Stream] Stream key validation result: {}", valid);
        return ResponseEntity.ok(valid);
    }

    @PostMapping("/{id}/restart")
    @Operation(
        summary = "Reiniciar stream encerrada",
        description = "Permite reiniciar uma stream com status ENDED, resetando métricas de viewers mas mantendo a stream key"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream reiniciada com sucesso",
            content = @Content(schema = @Schema(implementation = StreamPresenter.class))),
        @ApiResponse(responseCode = "404", description = "Stream não encontrada"),
        @ApiResponse(responseCode = "400", description = "Stream não pode ser reiniciada (status diferente de ENDED)")
    })
    public ResponseEntity<StreamPresenter> restartStream(
        @Parameter(description = "ID único da stream (UUID)")
        @PathVariable UUID id) {
        log.info("[Stream] Restarting stream - ID: {}", id);
        streamService.restartStream(id);
        var result = getStreamUseCase.execute(id);
        log.info("[Stream] Stream restarted successfully - ID: {}", id);
        return ResponseEntity.ok(StreamPresenter.from(result));
    }
}
