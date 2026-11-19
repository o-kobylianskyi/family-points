package com.olehkobylianskyi.familypoints.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Actual RSA-SHA256 signature verification + local HTTP JWK fetch via the
 * production GoogleTokenVerifier (not a mocked JwtDecoder or Google service).
 * All signing material is ephemeral and NEVER checked into the repository.
 */
class GoogleTokenVerifierLocalJwksCryptoTest {
    private static final String CLIENT="qa-google-client-id.apps.googleusercontent.com";

    @Test void realRs256JwksValidatesIssuerAudienceAzpSubjectSignatureAndTime() throws Exception {
        var generator=KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var signing=generator.generateKeyPair();
        var other=generator.generateKeyPair();
        var key=new RSAKey.Builder((java.security.interfaces.RSAPublicKey)signing.getPublic())
                .privateKey((java.security.interfaces.RSAPrivateKey)signing.getPrivate())
                .keyID("qa-key-1").algorithm(JWSAlgorithm.RS256).build();
        var wrongKey=new RSAKey.Builder((java.security.interfaces.RSAPublicKey)other.getPublic())
                .privateKey((java.security.interfaces.RSAPrivateKey)other.getPrivate())
                .keyID("qa-key-1").algorithm(JWSAlgorithm.RS256).build();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/jwks",exchange->{
            byte[] json=new JWKSet(key.toPublicJWK()).toString()
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,json.length);
            try(var out=exchange.getResponseBody()){out.write(json);}
        });
        server.start();
        try {
            var verifier=new GoogleTokenVerifier(CLIENT,
                    "http://127.0.0.1:"+server.getAddress().getPort()+"/jwks");
            Instant now=Instant.now();
            var valid=claims(now,"https://accounts.google.com",
                    List.of(CLIENT,"other-app"),CLIENT,"qa-subject");
            assertEquals("qa-subject",verifier.verifiedSubject(sign(key,valid,"qa-key-1")));
            assertEquals("qa-subject",verifier.verifiedSubject(sign(key,
                    claims(now,"accounts.google.com",List.of(CLIENT),null,"qa-subject"),
                    "qa-key-1")));

            assertDenied(verifier,sign(wrongKey,valid,"qa-key-1")); // wrong signature, same kid
            assertDenied(verifier,sign(key,valid,"unknown-kid"));
            assertDenied(verifier,sign(key,claims(now,"https://evil.invalid",
                    List.of(CLIENT),null,"qa-subject"),"qa-key-1"));
            assertDenied(verifier,sign(key,claims(now,"https://accounts.google.com",
                    List.of("other-app"),null,"qa-subject"),"qa-key-1"));
            assertDenied(verifier,sign(key,claims(now,"https://accounts.google.com",
                    List.of(CLIENT,"other-app"),"wrong-azp","qa-subject"),"qa-key-1"));
            assertDenied(verifier,sign(key,claims(now,"https://accounts.google.com",
                    List.of(CLIENT,"other-app"),null,"qa-subject"),"qa-key-1"));
            assertDenied(verifier,sign(key,claims(now,"https://accounts.google.com",
                    List.of(CLIENT),null," "),"qa-key-1"));
            assertDenied(verifier,sign(key,claims(now,"https://accounts.google.com",
                    List.of(CLIENT),null,"x".repeat(256)),"qa-key-1"));
            assertDenied(verifier,sign(key,claims(now.minusSeconds(3600),
                    "https://accounts.google.com",List.of(CLIENT),null,"qa-subject"),
                    "qa-key-1"));
            assertDenied(verifier,sign(key,claims(now.plusSeconds(3600),
                    "https://accounts.google.com",List.of(CLIENT),null,"qa-subject"),
                    "qa-key-1"));
        } finally {
            server.stop(0);
        }
    }

    private static JWTClaimsSet claims(Instant issued,String issuer,List<String> audience,
                                       String azp,String subject) {
        var builder=new JWTClaimsSet.Builder().issuer(issuer).audience(audience)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(issued.minusSeconds(30)))
                .expirationTime(Date.from(issued.plusSeconds(600)))
                .subject(subject);
        if(azp!=null)builder.claim("azp",azp);
        return builder.build();
    }
    private static String sign(RSAKey key,JWTClaimsSet claims,String kid) throws Exception {
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(kid).build(),claims);
        jwt.sign(new RSASSASigner(key.toPrivateKey()));
        return jwt.serialize();
    }
    private static void assertDenied(GoogleTokenVerifier verifier,String token) {
        assertThrows(ResponseStatusException.class,()->verifier.verifiedToken(token));
    }
}
