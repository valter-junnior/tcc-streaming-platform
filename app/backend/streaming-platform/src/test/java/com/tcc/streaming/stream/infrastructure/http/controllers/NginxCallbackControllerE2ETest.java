package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.streaming.AbstractE2ETest;
import com.tcc.streaming.stream.infrastructure.http.requests.CreateStreamRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E Tests for Nginx Callback Controller endpoints.
 * Tests the integration between Nginx-RTMP callbacks and the backend.
 */
@DisplayName("NginxCallbackController E2E Tests")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NginxCallbackControllerE2ETest extends AbstractE2ETest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/streams/callback/publish - Should authorize valid stream key")
    void shouldAuthorizeValidStreamKey() throws Exception {
        // Given - create a stream first
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "Nginx Callback Test Stream",
            "Test Nginx callback authorization"
        ,
            "test-owner-123"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamKey = objectMapper.readTree(responseBody).get("streamKey").asText();

        // When & Then
        mockMvc.perform(get("/api/streams/callback/publish")
                .param("name", streamKey))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/streams/callback/publish - Should reject invalid stream key")
    void shouldRejectInvalidStreamKey() throws Exception {
        // Given
        String invalidStreamKey = "invalid-stream-key-12345";

        // When & Then
        mockMvc.perform(get("/api/streams/callback/publish")
                .param("name", invalidStreamKey))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/streams/callback/publish_done - Should start stream successfully")
    void shouldStartStreamSuccessfully() throws Exception {
        // Given - create a stream first
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "Start Stream Test",
            "Test starting stream via callback"
        ,
            "test-owner-123"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamKey = objectMapper.readTree(responseBody).get("streamKey").asText();
        String streamId = objectMapper.readTree(responseBody).get("id").asText();

        // When - call publish_done callback
        mockMvc.perform(get("/api/streams/callback/publish_done")
                .param("name", streamKey))
            .andExpect(status().isOk());

        // Then - verify stream status changed to LIVE
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LIVE"))
            .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/streams/callback/done - Should end stream successfully")
    void shouldEndStreamSuccessfully() throws Exception {
        // Given - create and start a stream
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "End Stream Test",
            "Test ending stream via callback"
        ,
            "test-owner-123"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamKey = objectMapper.readTree(responseBody).get("streamKey").asText();
        String streamId = objectMapper.readTree(responseBody).get("id").asText();

        // Start the stream first
        mockMvc.perform(get("/api/streams/callback/publish_done")
                .param("name", streamKey))
            .andExpect(status().isOk());

        // When - call done callback
        mockMvc.perform(get("/api/streams/callback/done")
                .param("name", streamKey))
            .andExpect(status().isOk());

        // Then - verify stream status changed to ENDED
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENDED"))
            .andExpect(jsonPath("$.endedAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/streams/callback/done - Should return 200 even for non-existent stream")
    void shouldReturn200ForNonExistentStreamOnDone() throws Exception {
        // Given
        String nonExistentStreamKey = "non-existent-key";

        // When & Then - should not fail (Nginx expects 200)
        mockMvc.perform(get("/api/streams/callback/done")
                .param("name", nonExistentStreamKey))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Complete Nginx Flow - Publish, Publish Done, Done")
    void shouldCompleteFullNginxCallbackFlow() throws Exception {
        // 1. Create stream
        CreateStreamRequest createRequest = new CreateStreamRequest(
            "Full Nginx Flow Test",
            "Test complete Nginx callback flow"
        ,
            "test-owner-123"
        );

        MvcResult createResult = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String streamKey = objectMapper.readTree(responseBody).get("streamKey").asText();
        String streamId = objectMapper.readTree(responseBody).get("id").asText();

        // 2. Nginx calls /publish to validate (before accepting stream)
        mockMvc.perform(get("/api/streams/callback/publish")
                .param("name", streamKey))
            .andExpect(status().isOk());

        // 3. Verify stream is still WAITING
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"));

        // 4. Nginx calls /publish_done (stream actually started)
        mockMvc.perform(get("/api/streams/callback/publish_done")
                .param("name", streamKey))
            .andExpect(status().isOk());

        // 5. Verify stream is now LIVE
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LIVE"))
            .andExpect(jsonPath("$.startedAt").isNotEmpty());

        // 6. Nginx calls /done (stream ended)
        mockMvc.perform(get("/api/streams/callback/done")
                .param("name", streamKey))
            .andExpect(status().isOk());

        // 7. Verify stream is now ENDED
        mockMvc.perform(get("/api/streams/" + streamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENDED"))
            .andExpect(jsonPath("$.endedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle multiple streams independently")
    void shouldHandleMultipleStreamsIndependently() throws Exception {
        // Given - create two streams
        CreateStreamRequest request1 = new CreateStreamRequest("Stream 1", "First stream", "test-owner-123");
        CreateStreamRequest request2 = new CreateStreamRequest("Stream 2", "Second stream", "test-owner-123");

        MvcResult result1 = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated())
            .andReturn();

        MvcResult result2 = mockMvc.perform(post("/api/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isCreated())
            .andReturn();

        String streamKey1 = objectMapper.readTree(result1.getResponse().getContentAsString()).get("streamKey").asText();
        String streamId1 = objectMapper.readTree(result1.getResponse().getContentAsString()).get("id").asText();
        String streamKey2 = objectMapper.readTree(result2.getResponse().getContentAsString()).get("streamKey").asText();
        String streamId2 = objectMapper.readTree(result2.getResponse().getContentAsString()).get("id").asText();

        // When - start stream 1
        mockMvc.perform(get("/api/streams/callback/publish_done")
                .param("name", streamKey1))
            .andExpect(status().isOk());

        // Then - stream 1 should be LIVE, stream 2 should be WAITING
        mockMvc.perform(get("/api/streams/" + streamId1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LIVE"));

        mockMvc.perform(get("/api/streams/" + streamId2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"));

        // When - end stream 1 and start stream 2
        mockMvc.perform(get("/api/streams/callback/done")
                .param("name", streamKey1))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/streams/callback/publish_done")
                .param("name", streamKey2))
            .andExpect(status().isOk());

        // Then - stream 1 should be ENDED, stream 2 should be LIVE
        mockMvc.perform(get("/api/streams/" + streamId1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENDED"));

        mockMvc.perform(get("/api/streams/" + streamId2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LIVE"));
    }
}
