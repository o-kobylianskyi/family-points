package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Availability is advisory; registration and updates must enforce uniqueness atomically. */
@RestController
@RequestMapping("/auth")
public class UsernameAvailabilityController {
    private final JdbcTemplate db;

    public UsernameAvailabilityController(JdbcTemplate db) {
        this.db = db;
    }

    @GetMapping("/username-availability")
    public Map<String, Object> check(@RequestParam String username) {
        String normalized = username == null ? "" : username.trim();
        boolean valid = normalized.matches("[A-Za-z0-9._-]{3,64}");
        boolean available = false;
        if (valid) {
            Integer count = db.queryForObject(
                    "SELECT count(*) FROM user_accounts WHERE lower(username)=lower(?)",
                    Integer.class, normalized);
            available = count != null && count == 0;
        }
        return Map.of("valid", valid, "available", available);
    }
}
