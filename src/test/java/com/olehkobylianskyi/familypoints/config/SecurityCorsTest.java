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

    @Test
    void activationPreflightAllowsEpochAndIdempotencyHeadersOnBothRoutes() throws Exception {
        for (String kind : java.util.List.of("task", "reward")) {
            var request = new MockHttpServletRequest("OPTIONS",
                    "/workspaces/7/templates/42/activate-" + kind);
            request.addHeader("Origin", "https://beta.family-point.com");
            request.addHeader("Access-Control-Request-Method", "POST");
            request.addHeader("Access-Control-Request-Headers",
                    "authorization,content-type,x-workspace-data-epoch,idempotency-key");
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            assertThat(new org.springframework.web.cors.DefaultCorsProcessor()
                    .processRequest(source.getCorsConfiguration(request), request, response)).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://beta.family-point.com");
            assertThat(response.getHeader("Access-Control-Allow-Headers")).containsIgnoringCase("idempotency-key")
                    .containsIgnoringCase("x-workspace-data-epoch");
        }
    }

    @Test
    void activationPreflightStillRejectsUnlistedHeaders() throws Exception {
        var request = new MockHttpServletRequest("OPTIONS", "/workspaces/7/templates/42/activate-reward");
        request.addHeader("Origin", "https://beta.family-point.com");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "x-unlisted-header");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        assertThat(new org.springframework.web.cors.DefaultCorsProcessor()
                .processRequest(source.getCorsConfiguration(request), request, response)).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }
}
