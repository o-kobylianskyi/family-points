package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtWorkspaceClaimTest {
    private JwtService service() {
        byte[] bytes = "family-points-test-secret-key-material-long-enough".getBytes(StandardCharsets.UTF_8);
        return new JwtService(Base64.getEncoder().encodeToString(bytes), 3_600_000L);
    }

    @Test
    void signedWorkspaceClaimRoundTrips() {
        var jwt = service();
        var user = User.withUsername("tester").password("unused").roles("USER").build();
        String token = jwt.generateWorkspaceAccessToken(user, 27L);
        assertEquals("tester", jwt.extractUsername(token));
        assertEquals(27L, jwt.extractWorkspaceId(token));
        assertTrue(jwt.isTokenValid(token, user));
    }

    @Test
    void existingTokensRemainWithoutWorkspaceClaim() {
        var jwt = service();
        var user = User.withUsername("tester").password("unused").roles("USER").build();
        assertNull(jwt.extractWorkspaceId(jwt.generateAccessToken(user)));
    }

    @Test
    void refusesInvalidWorkspaceIds() {
        var jwt = service();
        var user = User.withUsername("tester").password("unused").roles("USER").build();
        assertThrows(IllegalArgumentException.class, () -> jwt.generateWorkspaceAccessToken(user, null));
        assertThrows(IllegalArgumentException.class, () -> jwt.generateWorkspaceAccessToken(user, 0L));
    }
}
