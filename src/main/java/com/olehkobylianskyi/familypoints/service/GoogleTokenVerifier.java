package com.olehkobylianskyi.familypoints.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Single source of truth for cryptographic Google identity verification. */
@Service
public class GoogleTokenVerifier {
    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
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
        if (clientId.isBlank() || idToken == null || idToken.isBlank()) {
            log.warn("GOOGLE_AUTH_MISSING_CONFIG_OR_TOKEN (clientIdConfigured={}, tokenPresent={})", !clientId.isBlank(), idToken != null && !idToken.isBlank());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google verification unavailable");
        }
        try {
            Jwt token = decoder.decode(idToken);
            if (token.getAudience().size() > 1
                    && !clientId.equals(token.getClaimAsString("azp"))) {
                log.warn("GOOGLE_AUTH_INVALID_AUTHORIZED_PARTY");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorized party");
            }
            String sub = token.getSubject();
            if (sub == null || sub.isBlank() || sub.length() > 255) {
                log.warn("GOOGLE_AUTH_INVALID_SUBJECT");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google identity");
            }
            return token;
        } catch (JwtValidationException ex) {
            log.warn("GOOGLE_AUTH_TOKEN_VALIDATION_FAILED (errorCodes={})", ex.getErrors().stream().map(e -> e.getErrorCode()).distinct().toList());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("GOOGLE_AUTH_TOKEN_DECODE_FAILED ({})", ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
    }
}
