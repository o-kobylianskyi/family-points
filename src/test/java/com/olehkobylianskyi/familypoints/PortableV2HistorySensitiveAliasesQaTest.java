package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.PortableV2HistoryDataPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QA-086 security-first alias boundary: the V2 raw-row privacy gate must
 * reject credential names in common external JSON and metadata conventions.
 * Red-first, isolated from production; pending Backend B policy review.
 */
class PortableV2HistorySensitiveAliasesQaTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> HISTORY = List.of(
            "POINT_TYPES", "TASK_DEFINITIONS", "REWARD_DEFINITIONS",
            "TASK_EXECUTIONS", "REWARD_PURCHASES", "POINT_LEDGER");
    private static final List<String> SECRET_ALIASES = List.of(
            "authorization_header", "set-cookie", "x-api-key",
            "privateKey", "refreshToken", "sessionKey",
            "clientSecret", "passwordHash");

    private static ObjectNode source() {
        var categories = JSON.createObjectNode();
        for (String name : HISTORY)
            categories.putArray(name).addObject()
                    .put("id", 1L).put("title", "Safe family history");
        return categories;
    }

    @Test
    void alternateCredentialNamesInEachHistoryCategoryAreRejected() {
        for (String category : HISTORY) {
            for (String alias : SECRET_ALIASES) {
                var rows = source();
                ((ObjectNode) rows.path(category).get(0))
                        .put(alias, "source-private-credential");
                assertThrows(SecurityException.class,
                        () -> PortableV2HistoryDataPolicy.requireAcceptable(rows),
                        category + "." + alias);
            }
        }
    }

    @Test
    void credentialAliasesInsideArrayMetadataAlsoFailClosed() {
        var rows = source();
        var item = ((ObjectNode) rows.path("TASK_EXECUTIONS").get(0))
                .putObject("metadata").putArray("http")
                .addObject();
        item.put("x-api-key", "source-private-credential");
        assertThrows(SecurityException.class,
                () -> PortableV2HistoryDataPolicy.requireAcceptable(rows));
    }

    @Test
    void legitimateCurrencyAndBusinessSourceFieldsRemainAccepted() {
        var rows = source();
        ((ObjectNode) rows.path("POINT_LEDGER").get(0))
                .put("source_type", "TASK").put("amount", 5);
        ((ObjectNode) rows.path("REWARD_DEFINITIONS").get(0))
                .put("price_amount", 4).put("reward_kind", "STANDARD");
        assertDoesNotThrow(() -> PortableV2HistoryDataPolicy.requireAcceptable(rows));
    }
}
