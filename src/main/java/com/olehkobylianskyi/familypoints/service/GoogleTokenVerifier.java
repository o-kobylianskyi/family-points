package com.olehkobylianskyi.familypoints.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Single source of truth for cryptographic Google identity verification. */
@Service
public class GoogleTokenVerifier {
    private final String clientId;
    private final JwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${familypoints.google.client-id:}") String clientId) {
        this.clientId = clientId;
        NimbusJwtDecoder verifier = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
        verifier.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<String>("iss", value ->
                        "https://accounts.google.com".equals(value) || "accounts.google.com".equals(value)),
                new JwtClaimValidator<List<String>>("aud", value ->
                        value != null && value.contains(clientId))));
        this.decoder = verifier;
    }

    public String verifiedSubject(String idToken) {
        return verifiedToken(idToken).getSubject();
    }

    public Jwt verifiedToken(String idToken) {
        if (clientId.isBlank() || idToken == null || idToken.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google verification unavailable");
        try {
            Jwt token = decoder.decode(idToken);
            if (token.getAudience().size() > 1
                    && !clientId.equals(token.getClaimAsString("azp")))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorized party");
            String sub = token.getSubject();
            if (sub == null || sub.isBlank() || sub.length() > 255)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google identity");
            return token;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
    }
}
