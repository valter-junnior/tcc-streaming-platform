package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.stream.core.dtos.stream.CreateStreamDto;
import com.tcc.streaming.stream.core.usecases.CreateStreamUseCase;
import com.tcc.streaming.stream.core.usecases.DeleteStreamUseCase;
import com.tcc.streaming.stream.core.usecases.GetStreamUseCase;
import com.tcc.streaming.stream.core.usecases.ValidateStreamKeyUseCase;
import com.tcc.streaming.stream.infrastructure.http.presenters.StreamPresenter;
import com.tcc.streaming.stream.infrastructure.http.requests.CreateStreamRequest;
import com.tcc.streaming.stream.infrastructure.http.requests.ValidateStreamKeyRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/streams")
public class StreamController {

    private final CreateStreamUseCase createStreamUseCase;
    private final GetStreamUseCase getStreamUseCase;
    private final DeleteStreamUseCase deleteStreamUseCase;
    private final ValidateStreamKeyUseCase validateStreamKeyUseCase;

    public StreamController(
        CreateStreamUseCase createStreamUseCase,
        GetStreamUseCase getStreamUseCase,
        DeleteStreamUseCase deleteStreamUseCase,
        ValidateStreamKeyUseCase validateStreamKeyUseCase
    ) {
        this.createStreamUseCase = createStreamUseCase;
        this.getStreamUseCase = getStreamUseCase;
        this.deleteStreamUseCase = deleteStreamUseCase;
        this.validateStreamKeyUseCase = validateStreamKeyUseCase;
    }

    @PostMapping
    public ResponseEntity<StreamPresenter> createStream(@Valid @RequestBody CreateStreamRequest request) {
        var dto = new CreateStreamDto(request.title(), request.description());
        var result = createStreamUseCase.execute(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(StreamPresenter.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StreamPresenter> getStream(@PathVariable UUID id) {
        var result = getStreamUseCase.execute(id);
        return ResponseEntity.ok(StreamPresenter.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStream(@PathVariable UUID id) {
        deleteStreamUseCase.deleteStream(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateStreamKey(@Valid @RequestBody ValidateStreamKeyRequest request) {
        boolean valid = validateStreamKeyUseCase.execute(request.streamKey());
        return ResponseEntity.ok(valid);
    }
}
