package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.streaming.AbstractE2ETest;
import com.tcc.streaming.stream.infrastructure.http.requests.CreateStreamRequest;
import com.tcc.streaming.stream.infrastructure.http.requests.ValidateStreamKeyRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E Tests for Stream Controller endpoints.
 * Tests the complete flow from HTTP request to database persistence.
 */
@DisplayName("StreamController E2E Tests")
class StreamControllerE2ETest extends AbstractE2ETest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/streams - Should create a new stream successfully")
    void shouldCreateStreamSuccessfully() throws Exception {
        // Given
        CreateStreamRequest request = new CreateStreamRequest(
            "Test Stream",
            "Test description for streaming"
        );

        // When & Then
        mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Test Stream"))
            .andExpect(jsonPath("$.description").value("Test description for streaming"))
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.streamKey").isNotEmpty())
            .andExpect(jsonPath("$.rtmpUrl").isNotEmpty())
            .andExpect(jsonPath("$.watchUrl").isNotEmpty())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.currentViewers").value(0))
            .andExpect(jsonPath("$.viewersPeak").value(0));
    }

    @Test
    @DisplayName("POST /api/streams - Should fail with invalid data")
    void shouldFailToCreateStreamWithInvalidData() throws Exception {
        // Given - empty title
        CreateStreamRequest request = new CreateStreamRequest("", "Description");

        // When & Then
        mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/streams/{id} - Should retrieve existing stream")
    void shouldRetrieveStreamById() throws Exception {
        // Given - create a stream first
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "Retrieval Test Stream",
            "Test retrieving stream by ID"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamId = objectMapper.readTree(responseBody).get("id").asText();

        // When & Then
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(streamId))
            .andExpect(jsonPath("$.title").value("Retrieval Test Stream"))
            .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("GET /api/streams/{id} - Should return 404 for non-existent stream")
    void shouldReturn404ForNonExistentStream() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/streams/" + nonExistentId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/streams/{id}/status - Should retrieve stream status")
    void shouldRetrieveStreamStatus() throws Exception {
        // Given - create a stream first
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "Status Test Stream",
            "Test retrieving stream status"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamId = objectMapper.readTree(responseBody).get("id").asText();

        // When & Then
        mockMvc.perform(get("/api/streams/" + streamId + "/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(streamId))
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.currentViewers").value(0))
            .andExpect(jsonPath("$.viewersPeak").value(0))
            .andExpect(jsonPath("$.watchUrl").isNotEmpty());
    }

    @Test
    @DisplayName("DELETE /api/streams/{id} - Should delete stream successfully")
    void shouldDeleteStreamSuccessfully() throws Exception {
        // Given - create a stream first
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "Delete Test Stream",
            "Test deleting stream"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamId = objectMapper.readTree(responseBody).get("id").asText();

        // When - delete the stream
        mockMvc.perform(delete("/api/streams/" + streamId))
            .andExpect(status().isNoContent());

        // Then - verify it was marked as ENDED
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENDED"));
    }

    @Test
    @DisplayName("DELETE /api/streams/{id} - Should return 404 for non-existent stream")
    void shouldReturn404WhenDeletingNonExistentStream() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/streams/" + nonExistentId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/streams/validate - Should validate existing stream key")
    void shouldValidateExistingStreamKey() throws Exception {
        // Given - create a stream first
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "Validate Test Stream",
            "Test validating stream key"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamKey = objectMapper.readTree(responseBody).get("streamKey").asText();

        ValidateStreamKeyRequest validateRequest = new ValidateStreamKeyRequest(streamKey);

        // When & Then
        mockMvc.perform(post("/api/streams/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /api/streams/validate - Should reject invalid stream key")
    void shouldRejectInvalidStreamKey() throws Exception {
        // Given - stream key with valid format (16 chars) but non-existent
        ValidateStreamKeyRequest validateRequest = new ValidateStreamKeyRequest("1234567890abcdef");

        // When & Then
        mockMvc.perform(post("/api/streams/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("POST /api/streams/validate - Should fail with empty stream key")
    void shouldFailValidationWithEmptyStreamKey() throws Exception {
        // Given
        ValidateStreamKeyRequest validateRequest = new ValidateStreamKeyRequest("");

        // When & Then
        mockMvc.perform(post("/api/streams/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Complete Flow - Create, Retrieve, Delete Stream")
    void shouldCompleteFullStreamLifecycle() throws Exception {
        // 1. Create stream
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "Lifecycle Test Stream",
            "Testing complete stream lifecycle"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamId = objectMapper.readTree(responseBody).get("id").asText();
        String streamKey = objectMapper.readTree(responseBody).get("streamKey").asText();

        // 2. Retrieve stream
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"));

        // 3. Check status
        mockMvc.perform(get("/api/streams/" + streamId + "/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"));

        // 4. Validate stream key
        ValidateStreamKeyRequest validateRequest = new ValidateStreamKeyRequest(streamKey);
        mockMvc.perform(post("/api/streams/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));

        // 5. Delete stream
        mockMvc.perform(delete("/api/streams/" + streamId))
            .andExpect(status().isNoContent());

        // 6. Verify it's ended
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENDED"));
    }
}
