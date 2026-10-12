package com.olehkobylianskyi.familypoints.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration}") long accessTokenExpiration
    ) {
        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000L;
    }

    public String generateAccessToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + accessTokenExpiration
        );

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Reserved for the coordinated multi-workspace authorization rollout.
     * Caller must verify membership with WorkspaceMembershipResolver first.
     * Existing authentication filters do not consume this claim yet.
     */
    public String generateWorkspaceAccessToken(UserDetails userDetails, Long workspaceId) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("A valid workspace ID is required");
        }
        Date now = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("workspaceId", workspaceId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(signingKey)
                .compact();
    }

    /** Returns null for legacy tokens without an active workspace claim. */
    public Long extractWorkspaceId(String token) {
        Object value = extractAllClaims(token).get("workspaceId");
        if (value == null) return null;
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Invalid workspace claim");
        }
        long workspaceId = number.longValue();
        if (workspaceId <= 0 || number.doubleValue() != (double) workspaceId) {
            throw new IllegalArgumentException("Invalid workspace claim");
        }
        return workspaceId;
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {
        String username = extractUsername(token);

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && userDetails.isEnabled();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}