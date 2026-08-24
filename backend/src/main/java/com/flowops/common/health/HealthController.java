package com.flowops.common.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness and readiness endpoints.
 *
 * <p>{@code /health} is a lightweight liveness probe. {@code /health/ready}
 * reports readiness of downstream dependencies; as the platform grows
 * (database, Redis, AI service) their checks are added to the {@code checks}
 * map here.
 */
@RestController
@Tag(name = "Health", description = "Service liveness and readiness")
public class HealthController {

    private static final String SERVICE = "flowops-api";
    private static final String VERSION = "0.1.0";

    @GetMapping("/health")
    @Operation(summary = "Liveness probe")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("service", SERVICE);
        body.put("version", VERSION);
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    @GetMapping("/health/ready")
    @Operation(summary = "Readiness probe (checks downstream dependencies)")
    public Map<String, Object> ready() {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("api", "ok");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ready");
        body.put("service", SERVICE);
        body.put("checks", checks);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
