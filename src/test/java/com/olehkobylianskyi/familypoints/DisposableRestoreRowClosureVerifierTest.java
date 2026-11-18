package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** B-B-08: read-only vNext original membership graph rehearsal. */
class DisposableRestoreRowClosureVerifierTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String SOURCE="""
      {
       "FAMILY_MEMBERS":[
         {"id":3,"family_id":17,"family_role_id":6,"member_type":"PARENT",
          "active":true,"deleted_at":null},
         {"id":4,"family_id":17,"family_role_id":7,"member_type":"CHILD",
          "active":false,"deleted_at":"2026-11-01T12:00:00"}
       ],
       "TASK_DEFINITIONS":[{"id":11,"family_id":17}],
       "TASK_EXECUTIONS":[{"id":21,"member_id":4,"task_definition_id":11,
                           "original_assigned_member_id":3}],
       "REWARD_DEFINITIONS":[{"id":31,"family_id":17}],
       "REWARD_PURCHASES":[{"id":41,"member_id":4,"reward_definition_id":31,
                            "point_type_id":51,"price_amount":4}],
       "POINT_TYPES":[{"id":51,"family_id":17,"code":"POINTS"}],
       "POINT_LEDGER":[
         {"id":61,"member_id":4,"point_type_id":51,"amount":5,
          "type":"EARN","source_type":"TASK","source_id":21},
         {"id":62,"member_id":4,"point_type_id":51,"amount":-4,
          "type":"SPEND","source_type":"REWARD_PURCHASE","source_id":41},
         {"id":63,"member_id":4,"point_type_id":51,"amount":2,
          "type":"EARN","source_type":"MANUAL","source_id":null}
       ]
      }
      """;
    private static DisposableRestoreFinancialDependencyPreflight.Mapping mapping() {
        return new DisposableRestoreFinancialDependencyPreflight.Mapping(
                Map.of(3L,103L,4L,104L),Map.of(51L,151L),
                Map.of(11L,111L),Map.of(31L,131L),Map.of(41L,141L));
    }
    private static ObjectNode payload() throws Exception {
        return (ObjectNode)JSON.readTree(SOURCE);
    }
    private static void denied(ObjectNode input) {
        assertThrows(SecurityException.class,()->
                new DisposableRestoreRowClosureVerifier().verify(input,17,mapping()));
    }

    @Test void deletedMemberCanOwnHistoricalEventsWithoutNewLogin() throws Exception {
        var result=new DisposableRestoreRowClosureVerifier()
                .verify(payload(),17,mapping());
        assertEquals(2,result.originalMembers());
        assertEquals(1,result.taskExecutions());
        assertEquals(1,result.purchases());
        assertEquals(3,result.ledgerRows());
        assertEquals(Map.of("104:151",3L),result.mappedBalances());
    }

    @Test void missingRosterForeignMemberAndMissingHistoricalAssigneeDeny()
            throws Exception {
        var noRoster=payload();noRoster.remove("FAMILY_MEMBERS");
        denied(noRoster);
        var wrongFamily=payload();
        ((ObjectNode)wrongFamily.path("FAMILY_MEMBERS").get(1))
                .put("family_id",18);
        denied(wrongFamily);
        var orphan=payload();
        ((ObjectNode)orphan.path("TASK_EXECUTIONS").get(0))
                .put("original_assigned_member_id",999);
        denied(orphan);
        var duplicated=payload();
        var roster=(com.fasterxml.jackson.databind.node.ArrayNode)
                duplicated.path("FAMILY_MEMBERS");
        roster.add(roster.get(0).deepCopy());
        denied(duplicated);
    }

    @Test void orphanTaskPurchaseCurrencyAndLedgerReferencesDeny()
            throws Exception {
        var missingTask=payload();
        ((ObjectNode)missingTask.path("TASK_EXECUTIONS").get(0))
                .put("task_definition_id",999);
        denied(missingTask);
        var foreignReward=payload();
        ((ObjectNode)foreignReward.path("REWARD_DEFINITIONS").get(0))
                .put("family_id",19);
        denied(foreignReward);
        var missingPurchase=payload();
        ((ObjectNode)missingPurchase.path("POINT_LEDGER").get(1))
                .put("source_id",999);
        denied(missingPurchase);
        var foreignCurrency=payload();
        ((ObjectNode)foreignCurrency.path("POINT_LEDGER").get(0))
                .put("point_type_id",999);
        denied(foreignCurrency);
    }

    @Test void unsupportedRefundOrRequestOwnedPurchaseFailsBeforeTargetWrite()
            throws Exception {
        var refund=payload();
        ((ObjectNode)refund.path("POINT_LEDGER").get(0))
                .put("source_type","REFUND");
        denied(refund);
        var request=payload();
        ((ObjectNode)request.path("REWARD_PURCHASES").get(0))
                .put("reward_request_id",77);
        denied(request);
    }

    @Test void changedMemberMapCannotClaimCompleteRoster() throws Exception {
        var limited=new DisposableRestoreFinancialDependencyPreflight.Mapping(
                Map.of(4L,104L),Map.of(51L,151L),Map.of(11L,111L),
                Map.of(31L,131L),Map.of(41L,141L));
        assertThrows(SecurityException.class,()->
                new DisposableRestoreRowClosureVerifier()
                        .verify(payload(),17,limited));
    }
}
