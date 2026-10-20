package com.example.authapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health", description = "Application health and readiness probe")
@SecurityRequirements
public class HealthController {

    @GetMapping({"/", "/health", "/api/health"})
    @Operation(summary = "Health probe endpoint", description = "Lightweight unauthenticated probe for hosting platforms")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "FlowZenix API",
            "documentation", "/swagger-ui/index.html"
        ));
    }
}
