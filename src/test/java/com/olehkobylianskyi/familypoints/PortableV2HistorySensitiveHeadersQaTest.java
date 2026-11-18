package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.PortableV2HistoryDataPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QA-078 red-first privacy regression, intentionally isolated from main.
 * Unknown HTTP auth/session fields must not leave the source database
 * merely because their keys are not yet on a narrow deny-list.
 */
class PortableV2HistorySensitiveHeadersQaTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> CATEGORIES = List.of(
            "POINT_TYPES", "TASK_DEFINITIONS", "REWARD_DEFINITIONS",
            "TASK_EXECUTIONS", "REWARD_PURCHASES", "POINT_LEDGER");
    private static final List<String> PRIVATE_HEADERS = List.of(
            "authorization", "cookie", "set_cookie", "private_key");

    private static ObjectNode validCategories() {
        var categories = JSON.createObjectNode();
        for (String category : CATEGORIES)
            categories.putArray(category).addObject()
                    .put("id", 1L).put("description", "Family history");
        return categories;
    }

    @Test
    void allHistoricalCategoriesRejectHttpCredentialsAndPrivateKeys() {
        for (String category : CATEGORIES) {
            for (String privateField : PRIVATE_HEADERS) {
                var source = validCategories();
                ((ObjectNode) source.path(category).get(0))
                        .put(privateField, "Bearer source-only-private-value");
                assertThrows(SecurityException.class,
                        () -> PortableV2HistoryDataPolicy.requireAcceptable(source),
                        category + "." + privateField + " must not be exported");
            }
        }
    }

    @Test
    void nestedAuthorizationHeadersAlsoFailClosed() {
        var categories = validCategories();
        ((ObjectNode) categories.path("TASK_EXECUTIONS").get(0))
                .putObject("metadata")
                .putObject("http_headers")
                .put("Authorization", "Bearer source-only-private-value");
        assertThrows(SecurityException.class,
                () -> PortableV2HistoryDataPolicy.requireAcceptable(categories));
    }

    @Test
    void normalBusinessDescriptionRemainsAcceptable() {
        assertDoesNotThrow(
                () -> PortableV2HistoryDataPolicy.requireAcceptable(validCategories()));
    }
}
