package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.stream.application.services.StreamService;
import com.tcc.streaming.stream.core.dtos.stream.CreateStreamDto;
import com.tcc.streaming.stream.core.dtos.stream.UpdateStreamDto;
import com.tcc.streaming.stream.core.usecases.CreateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.DeleteStreamUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamStatusUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ListLiveStreamsUseCase;
import com.tcc.streaming.stream.core.usecases.ListUserStreamsUseCase;
import com.tcc.streaming.stream.core.usecases.UpdateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ValidateStreamKeyUseCase;
import com.tcc.streaming.stream.infrastructure.http.presenters.StreamPresenter;
import com.tcc.streaming.stream.infrastructure.http.presenters.StreamStatusPresenter;
import com.tcc.streaming.stream.infrastructure.http.requests.CreateStreamRequest;
import com.tcc.streaming.stream.infrastructure.http.requests.UpdateStreamRequest;
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
    private final ListUserStreamsUseCase listUserStreamsUseCase;
    private final UpdateStreamUseCase updateStreamUseCase;
    private final StreamService streamService;

    public StreamController(
        CreateStreamUseCase createStreamUseCase,
        GetStreamUseCase getStreamUseCase,
        GetStreamStatusUseCase getStreamStatusUseCase,
        DeleteStreamUseCase deleteStreamUseCase,
        ValidateStreamKeyUseCase validateStreamKeyUseCase,
        ListLiveStreamsUseCase listLiveStreamsUseCase,
        ListUserStreamsUseCase listUserStreamsUseCase,
        UpdateStreamUseCase updateStreamUseCase,
        StreamService streamService
    ) {
        this.createStreamUseCase = createStreamUseCase;
        this.getStreamUseCase = getStreamUseCase;
        this.getStreamStatusUseCase = getStreamStatusUseCase;
        this.deleteStreamUseCase = deleteStreamUseCase;
        this.validateStreamKeyUseCase = validateStreamKeyUseCase;
        this.listLiveStreamsUseCase = listLiveStreamsUseCase;
        this.listUserStreamsUseCase = listUserStreamsUseCase;
        this.updateStreamUseCase = updateStreamUseCase;
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
        log.info("[Stream] Creating new stream - Title: {}, OwnerId: {}", request.title(), request.ownerId());
        var dto = new CreateStreamDto(request.title(), request.description(), request.ownerId());
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
        var result = listLiveStreamsUseCase.execute();
        var presenters = result.stream()
            .map(StreamPresenter::from)
            .collect(java.util.stream.Collectors.toList());
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

    @GetMapping("/my")
    @Operation(
        summary = "Listar minhas streams",
        description = "Retorna todas as streams criadas por um usuário específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de streams retornada com sucesso",
            content = @Content(schema = @Schema(implementation = StreamPresenter.class))),
        @ApiResponse(responseCode = "400", description = "Owner ID inválido")
    })
    public ResponseEntity<List<StreamPresenter>> getMyStreams(
        @Parameter(description = "ID do usuário dono das streams", required = true)
        @RequestParam String ownerId) {
        log.info("[Stream] Fetching streams for owner: {}", ownerId);
        var result = listUserStreamsUseCase.listByOwner(ownerId);
        log.info("[Stream] Found {} streams for owner: {}", result.size(), ownerId);
        return ResponseEntity.ok(
            result.stream()
                .map(StreamPresenter::from)
                .toList()
        );
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar stream",
        description = "Atualiza título e descrição de uma stream. Requer ownership."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = StreamPresenter.class))),
        @ApiResponse(responseCode = "404", description = "Stream não encontrada"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para editar esta stream"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<StreamPresenter> updateStream(
        @Parameter(description = "ID único da stream (UUID)")
        @PathVariable UUID id,
        @Parameter(description = "Dados para atualização")
        @Valid @RequestBody UpdateStreamRequest request) {
        log.info("[Stream] Updating stream {} by owner {}", id, request.ownerId());
        var dto = new UpdateStreamDto(id, request.ownerId(), request.title(), request.description());
        var result = updateStreamUseCase.update(dto);
        log.info("[Stream] Stream {} updated successfully", id);
        return ResponseEntity.ok(StreamPresenter.from(result));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deletar stream",
        description = "Deleta permanentemente uma stream. Requer ownership e que a stream não esteja LIVE."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Stream deletada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Stream não encontrada"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para deletar esta stream"),
        @ApiResponse(responseCode = "400", description = "Stream está ao vivo, finalize antes de deletar")
    })
    public ResponseEntity<Void> deleteStream(
        @Parameter(description = "ID único da stream (UUID)")
        @PathVariable UUID id,
        @Parameter(description = "ID do usuário dono da stream", required = true)
        @RequestParam String ownerId) {
        log.info("[Stream] Deleting stream {} by owner {}", id, ownerId);
        deleteStreamUseCase.deleteStream(id, ownerId);
        log.info("[Stream] Stream {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}
