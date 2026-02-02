package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.stream.application.services.StreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streams/callback")
public class NginxCallbackController {

    private final StreamService streamService;

    public NginxCallbackController(StreamService streamService) {
        this.streamService = streamService;
    }

    /**
     * Nginx-RTMP callback quando streamer tenta publicar
     * GET /api/streams/callback/publish?name=STREAM_KEY
     */
    @GetMapping("/publish")
    public ResponseEntity<Void> onPublish(@RequestParam String name) {
        boolean valid = streamService.execute(name);
        return valid ? ResponseEntity.ok().build() : ResponseEntity.status(403).build();
    }

    /**
     * Nginx-RTMP callback quando stream começa a transmitir
     * GET /api/streams/callback/publish_done?name=STREAM_KEY
     */
    @GetMapping("/publish_done")
    public ResponseEntity<Void> onPublishDone(@RequestParam String name) {
        streamService.startStream(name);
        return ResponseEntity.ok().build();
    }

    /**
     * Nginx-RTMP callback quando stream termina
     * GET /api/streams/callback/done?name=STREAM_KEY
     */
    @GetMapping("/done")
    public ResponseEntity<Void> onDone(@RequestParam String name) {
        try {
            streamService.endStream(name);
        } catch (Exception e) {
            // Log error but return 200 to Nginx
        }
        return ResponseEntity.ok().build();
    }
}
