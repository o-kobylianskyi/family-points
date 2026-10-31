package com.olehkobylianskyi.familypoints.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public application-identity probe for the Android client.
 * Does not expose internal diagnostics, workspace or user data.
 */
@RestController
public class HealthController {
    @GetMapping("/api/client-health")
    public Map<String, String> clientHealth() {
        return Map.of("service", "family-points", "status", "UP");
    }
}
