package com.olehkobylianskyi.familypoints.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityCorsTest {
    private final CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource();

    private CorsConfiguration cors() {
        var request = new MockHttpServletRequest("OPTIONS", "/auth/google/register");
        return source.getCorsConfiguration(request);
    }

    @Test
    void publicFamilyPointsOriginsAreAllowed() {
        assertThat(cors().checkOrigin("https://family-point.com")).isEqualTo("https://family-point.com");
        assertThat(cors().checkOrigin("https://beta.family-point.com")).isEqualTo("https://beta.family-point.com");
        assertThat(cors().checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
    }

    @Test
    void unrelatedHttpsSitesAreNotAllowed() {
        assertThat(cors().checkOrigin("https://unrelated.example")).isNull();
        assertThat(cors().checkOrigin("http://family-point.com")).isNull();
    }
}
