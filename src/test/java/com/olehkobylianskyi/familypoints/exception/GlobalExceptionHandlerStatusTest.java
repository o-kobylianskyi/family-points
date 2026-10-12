package com.olehkobylianskyi.familypoints.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerStatusTest {
    @Test
    void cooldownProducesConflictResponseNotAuthenticationError() {
        var response = new GlobalExceptionHandler().handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "Reward is on cooldown. Try again later."));
        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Reward is on cooldown. Try again later.", response.getBody().get("message"));
    }
}
