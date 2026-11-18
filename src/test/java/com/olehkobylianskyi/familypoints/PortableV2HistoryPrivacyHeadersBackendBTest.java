package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.PortableV2HistoryDataPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Backend B acceptance for QA-078. Independently checks sensitive keys
 * in all six history categories and deeply nested metadata.
 * No database, restore, or production data is involved.
 */
class PortableV2HistoryPrivacyHeadersBackendBTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> CATEGORIES = List.of(
            "POINT_TYPES", "TASK_DEFINITIONS", "REWARD_DEFINITIONS",
            "TASK_EXECUTIONS", "REWARD_PURCHASES", "POINT_LEDGER");
    private static final List<String> PRIVATE_FIELDS = List.of(
            "authorization", "cookie", "set_cookie", "private_key",
            "proxy_authorization", "client_secret", "access_key", "session_key",
            "signing_private_key", "integration_api_key");

    private static ObjectNode validCategories() {
        var categories = JSON.createObjectNode();
        for (String category : CATEGORIES)
            categories.putArray(category).addObject()
                    .put("id", 1L).put("description", "Family history");
        return categories;
    }

    @Test
    void rejectsEverySensitiveKeyAcrossAllSixCategories() {
        for (String category : CATEGORIES) {
            for (String privateField : PRIVATE_FIELDS) {
                var source = validCategories();
                ((ObjectNode) source.path(category).get(0))
                        .put(privateField, "source-only-secret");
                assertThrows(SecurityException.class,
                        () -> PortableV2HistoryDataPolicy.requireAcceptable(source),
                        category + "." + privateField);
            }
        }
    }

    @Test
    void nestedCaseInsensitiveAuthorizationFailsClosed() {
        var source = validCategories();
        ((ObjectNode) source.path("TASK_EXECUTIONS").get(0))
                .putObject("metadata").putObject("http_headers")
                .put("Authorization", "Bearer source-only-secret");
        assertThrows(SecurityException.class,
                () -> PortableV2HistoryDataPolicy.requireAcceptable(source));
    }

    @Test
    void existingAuthenticationFieldsAndNormalBusinessData() {
        var valid = validCategories();
        assertDoesNotThrow(() -> PortableV2HistoryDataPolicy.requireAcceptable(valid));
        for (String key : List.of("email", "password_hash", "refresh_token")) {
            var invalid = validCategories();
            ((ObjectNode) invalid.path("POINT_LEDGER").get(0)).put(key, "secret");
            assertThrows(SecurityException.class,
                    () -> PortableV2HistoryDataPolicy.requireAcceptable(invalid), key);
        }
    }
}
