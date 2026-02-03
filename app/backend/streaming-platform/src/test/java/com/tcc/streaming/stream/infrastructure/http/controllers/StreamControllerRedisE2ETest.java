package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.AbstractE2ETestWithRedis;
import com.tcc.streaming.stream.infrastructure.http.requests.CreateStreamRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E tests for Stream endpoints with REAL Redis cache.
 * These tests validate that:
 * - JSON serialization/deserialization works correctly with Redis
 * - Cache operations don't break with LocalDateTime and other Java 8 types
 * - The application behaves correctly in production-like environment
 */
@DisplayName("Stream Controller E2E Tests with Redis Cache")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StreamControllerRedisE2ETest extends AbstractE2ETestWithRedis {

    @Test
    @DisplayName("Should create stream and cache it in Redis")
    void shouldCreateStreamAndCacheInRedis() throws Exception {
        // Given
        CreateStreamRequest request = new CreateStreamRequest(
            "Test Stream with Redis",
            "Testing Redis serialization"
        ,
            "test-owner-123"
        );

        // When - Create stream
        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Test Stream with Redis"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamId = objectMapper.readTree(responseBody).get("id").asText();

        // Then - First GET should fetch from DB and cache in Redis
        mockMvc.perform(get("/api/streams/{id}", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(streamId))
            .andExpect(jsonPath("$.title").value("Test Stream with Redis"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.startedAt").isEmpty())
            .andExpect(jsonPath("$.endedAt").isEmpty())
            .andExpect(jsonPath("$.status").value("WAITING"));

        // And - Second GET should fetch from Redis cache (no DB query)
        mockMvc.perform(get("/api/streams/{id}", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(streamId))
            .andExpect(jsonPath("$.title").value("Test Stream with Redis"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("Should get stream status from Redis cache")
    void shouldGetStreamStatusFromRedisCache() throws Exception {
        // Given - Create a stream
        CreateStreamRequest request = new CreateStreamRequest(
            "Status Cache Test",
            "Testing status endpoint with Redis"
        ,
            "test-owner-123"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String streamId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id").asText();

        // When - Get status (should cache it)
        mockMvc.perform(get("/api/streams/{id}/status", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(streamId))
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.currentViewers").value(0))
            .andExpect(jsonPath("$.viewersPeak").value(0));

        // Then - Second call should use cache
        mockMvc.perform(get("/api/streams/{id}/status", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(streamId))
            .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("Should handle multiple streams with Redis cache")
    void shouldHandleMultipleStreamsWithRedisCache() throws Exception {
        // Given - Create multiple streams
        String[] streamIds = new String[3];
        
        for (int i = 0; i < 3; i++) {
            CreateStreamRequest request = new CreateStreamRequest(
                "Stream " + (i + 1),
                "Description " + (i + 1),
                "test-owner-123"
            );

            MvcResult result = mockMvc.perform(post("/api/streams")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

            streamIds[i] = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
        }

        // When/Then - Fetch all streams (should cache them)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/streams/{id}", streamIds[i]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(streamIds[i]))
                .andExpect(jsonPath("$.title").value("Stream " + (i + 1)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.status").value("WAITING"));
        }

        // And - Fetch again from cache
        for (String streamId : streamIds) {
            mockMvc.perform(get("/api/streams/{id}", streamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(streamId))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }
    }

    @Test
    @DisplayName("Should evict cache when deleting stream")
    void shouldEvictCacheWhenDeletingStream() throws Exception {
        // Given - Create and cache a stream
        CreateStreamRequest request = new CreateStreamRequest(
            "Stream to Delete",
            "Testing cache eviction"
        ,
            "test-owner-123"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String streamId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id").asText();

        // Cache the stream
        mockMvc.perform(get("/api/streams/{id}", streamId))
            .andExpect(status().isOk());

        // When - Delete stream (should evict cache)
        mockMvc.perform(delete("/api/streams/{id}", streamId))
            .andExpect(status().isNoContent());

        // Then - Stream should still exist but be marked as ENDED
        mockMvc.perform(get("/api/streams/{id}", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENDED"));
    }

    @Test
    @DisplayName("Should serialize LocalDateTime correctly in Redis")
    void shouldSerializeLocalDateTimeCorrectlyInRedis() throws Exception {
        // Given
        CreateStreamRequest request = new CreateStreamRequest(
            "DateTime Test",
            "Validating LocalDateTime serialization"
        ,
            "test-owner-123"
        );

        // When - Create stream
        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String streamId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id").asText();

        // Then - GET should return with properly formatted createdAt (ISO-8601)
        mockMvc.perform(get("/api/streams/{id}", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.?\\d*")))
            .andExpect(jsonPath("$.startedAt").isEmpty())
            .andExpect(jsonPath("$.endedAt").isEmpty());
    }

    @Test
    @DisplayName("Should complete full lifecycle with Redis cache")
    void shouldCompleteFullLifecycleWithRedisCache() throws Exception {
        // Given - Create stream
        CreateStreamRequest request = new CreateStreamRequest(
            "Lifecycle Test",
            "Testing full lifecycle with Redis"
        ,
            "test-owner-123"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andReturn();

        String streamId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id").asText();

        // When - Get stream (caches it)
        mockMvc.perform(get("/api/streams/{id}", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        // And - Get status (caches status separately)
        mockMvc.perform(get("/api/streams/{id}/status", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"));

        // And - Validate stream key
        String streamKey = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("streamKey").asText();

        mockMvc.perform(post("/api/streams/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"streamKey\":\"" + streamKey + "\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));

        // Then - Delete stream (evicts cache)
        mockMvc.perform(delete("/api/streams/{id}", streamId))
            .andExpect(status().isNoContent());

        // And - Verify it's marked as ENDED (not deleted from database)
        mockMvc.perform(get("/api/streams/{id}", streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENDED"));
    }
}
