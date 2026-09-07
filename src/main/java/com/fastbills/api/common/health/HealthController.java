package com.fastbills.api.common.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Application health and status endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check API health status", description = "Returns system operational status and timestamp")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "fastbills-api",
                "version", "0.0.1-SNAPSHOT",
                "timestamp", Instant.now()
        ));
    }
}
