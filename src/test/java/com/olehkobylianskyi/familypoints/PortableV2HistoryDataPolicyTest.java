package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.PortableV2HistoryDataPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortableV2HistoryDataPolicyTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final List<String> CATEGORIES=List.of(
            "POINT_TYPES","TASK_DEFINITIONS","REWARD_DEFINITIONS",
            "TASK_EXECUTIONS","REWARD_PURCHASES","POINT_LEDGER");

    private static ObjectNode allCategories() {
        var result=JSON.createObjectNode();
        for(String name:CATEGORIES) {
            var array=result.putArray(name);
            array.addObject().put("id",1L).put("description","Family-supplied history");
        }
        return result;
    }

    @Test void legitimateTaskAndFinanceBusinessHistoryAreRetained() {
        var rows=allCategories();
        ((ObjectNode)rows.path("TASK_EXECUTIONS").get(0))
                .put("original_assigned_member_id",21L)
                .put("claimed_at","2026-11-18T09:00:00")
                .put("reward_amount",5)
                .putNull("penalty_point_type_id");
        ((ObjectNode)rows.path("POINT_LEDGER").get(0))
                .put("source_type","TASK")
                .put("client_operation_id","11111111-2222-4333-8444-555555555555");
        assertDoesNotThrow(()->PortableV2HistoryDataPolicy.requireAcceptable(rows));
        assertEquals("Family-supplied history",
                rows.path("POINT_LEDGER").get(0).path("description").asText());
    }

    @Test void everyHistoryCategoryDeniesPrivateAccountAndAuthenticationColumns() {
        for(String category:CATEGORIES) {
            for(String secret:List.of("email","password_hash","refresh_token",
                    "account_id","provider_subject","oauth_subject","device_id")) {
                var rows=allCategories();
                ((ObjectNode)rows.path(category).get(0)).put(secret,"confidential");
                assertThrows(SecurityException.class,
                        ()->PortableV2HistoryDataPolicy.requireAcceptable(rows),
                        category+"."+secret);
            }
        }
    }

    @Test void nestedSecretsInvalidShapeAndOversizedTextFailClosed() {
        var nested=allCategories();
        var payload=((ObjectNode)nested.path("TASK_EXECUTIONS").get(0))
                .putObject("metadata").putObject("nested");
        payload.put("api_key","sensitive");
        assertThrows(SecurityException.class,
                ()->PortableV2HistoryDataPolicy.requireAcceptable(nested));

        var badRows=allCategories();
        ((com.fasterxml.jackson.databind.node.ArrayNode)badRows.path("POINT_LEDGER"))
                .add("not-an-object");
        assertThrows(SecurityException.class,
                ()->PortableV2HistoryDataPolicy.requireAcceptable(badRows));

        var oversized=allCategories();
        ((ObjectNode)oversized.path("REWARD_DEFINITIONS").get(0))
                .put("description","x".repeat(16_385));
        assertThrows(SecurityException.class,
                ()->PortableV2HistoryDataPolicy.requireAcceptable(oversized));
    }

    @Test void nestedHttpHeaderAliasesAndHyphenatedSecretsFailClosed() {
        for(String category:CATEGORIES) {
            for(String alias:List.of("authorization_header","x-api-key",
                    "http_authorization","nested_private_key")) {
                var rows=allCategories();
                ((ObjectNode)rows.path(category).get(0))
                        .putObject("metadata")
                        .putArray("http")
                        .addObject().put(alias,"sensitive");
                assertThrows(SecurityException.class,
                        ()->PortableV2HistoryDataPolicy.requireAcceptable(rows),
                        category+"."+alias);
            }
        }
    }

    @Test void unknownOrMissingHistoricalCategoryIsNeverTreatedAsComplete() {
        var source=allCategories();
        source.remove("REWARD_PURCHASES");
        assertThrows(SecurityException.class,
                ()->PortableV2HistoryDataPolicy.requireAcceptable(source));
    }
}
