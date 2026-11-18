package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** No DB writes, no production finance replay. */
class DisposableRestoreFinancialDependencyPreflightTest {
    private final ObjectMapper json=new ObjectMapper();
    private final DisposableRestoreFinancialDependencyPreflight verifier=
            new DisposableRestoreFinancialDependencyPreflight();

    private static final String ARCHIVE="""
            {
              "TASK_DEFINITIONS":[{"id":11,"family_id":17}],
              "TASK_EXECUTIONS":[{"id":21,"member_id":3,"task_definition_id":11}],
              "REWARD_DEFINITIONS":[{"id":31,"family_id":17}],
              "REWARD_PURCHASES":[{"id":41,"member_id":3,"reward_definition_id":31,
                                    "point_type_id":51,"price_amount":4}],
              "POINT_TYPES":[{"id":51,"family_id":17,"code":"POINTS"}],
              "POINT_LEDGER":[
                {"id":61,"member_id":3,"point_type_id":51,"amount":5,"type":"EARN",
                 "source_type":"TASK","source_id":21},
                {"id":62,"member_id":3,"point_type_id":51,"amount":-4,"type":"SPEND",
                 "source_type":"REWARD_PURCHASE","source_id":41},
                {"id":63,"member_id":3,"point_type_id":51,"amount":2,"type":"EARN",
                 "source_type":"MANUAL","source_id":null}
              ]
            }
            """;

    private static DisposableRestoreFinancialDependencyPreflight.Mapping mapping() {
        return new DisposableRestoreFinancialDependencyPreflight.Mapping(
                Map.of(3L,103L),Map.of(51L,151L),
                Map.of(11L,111L),Map.of(31L,131L),Map.of(41L,141L));
    }
    private JsonNode parse(String text) throws Exception {return json.readTree(text);}
    private void denied(String archive,DisposableRestoreFinancialDependencyPreflight.Mapping maps)
            throws Exception {
        assertThrows(SecurityException.class,()->verifier.verify(parse(archive),17,maps));
    }

    @Test void taskAwardPurchaseSpendAndManualRemainBoundToExplicitTargetIds()
            throws Exception {
        var result=verifier.verify(parse(ARCHIVE),17,mapping());
        assertEquals(1,result.taskExecutions());
        assertEquals(1,result.purchases());
        assertEquals(3,result.ledgerRows());
        assertEquals(Map.of("103:151",3L),result.expectedTargetBalances());
    }

    @Test void missingTaskOrRewardParentsAreDenied() throws Exception {
        denied(ARCHIVE.replace("\"task_definition_id\":11",
                "\"task_definition_id\":999"),mapping());
        denied(ARCHIVE.replace("\"reward_definition_id\":31",
                "\"reward_definition_id\":999"),mapping());
        denied(ARCHIVE.replace("\"family_id\":17",
                "\"family_id\":18"),mapping());
    }

    @Test void foreignMemberAndCurrencyAndIncompleteMapsAreDenied() throws Exception {
        denied(ARCHIVE.replace("\"member_id\":3",
                "\"member_id\":999"),mapping());
        denied(ARCHIVE.replace("\"point_type_id\":51,\"amount\":5",
                "\"point_type_id\":999,\"amount\":5"),mapping());
        var reduced=new DisposableRestoreFinancialDependencyPreflight.Mapping(
                Map.of(),Map.of(51L,151L),Map.of(11L,111L),
                Map.of(31L,131L),Map.of(41L,141L));
        denied(ARCHIVE,reduced);
        var missingPurchase=new DisposableRestoreFinancialDependencyPreflight.Mapping(
                Map.of(3L,103L),Map.of(51L,151L),Map.of(11L,111L),
                Map.of(31L,131L),Map.of());
        denied(ARCHIVE,missingPurchase);
    }

    @Test void duplicateAndMismatchedPurchaseLegCannotCreateFreePoints()
            throws Exception {
        denied(ARCHIVE.replace("\"amount\":-4",
                "\"amount\":-3"),mapping());
        denied(ARCHIVE.replace("\"type\":\"SPEND\"",
                "\"type\":\"EARN\""),mapping());
        denied(ARCHIVE.replace("\"source_id\":41",
                "\"source_id\":999"),mapping());
        denied(ARCHIVE.replace("\"id\":63", "\"id\":62"),mapping());
        denied(ARCHIVE.replace("\"source_type\":\"MANUAL\"",
                "\"source_type\":\"REFUND\""),mapping());
    }

    @Test void unsupportedRefundAndExchangeCannotBeMisreadAsManual()
            throws Exception {
        denied(ARCHIVE.replace("\"source_type\":\"TASK\"",
                "\"source_type\":\"POINT_EXCHANGE\""),mapping());
        denied(ARCHIVE.replace("\"source_type\":\"REWARD_PURCHASE\"",
                "\"source_type\":\"REFUND\""),mapping());
    }

    @Test void missingCategoryOrDuplicateTargetIdFailsClosed() throws Exception {
        denied(ARCHIVE.replace("\"POINT_TYPES\":[{\"id\":51,\"family_id\":17,\"code\":\"POINTS\"}],",
                ""),mapping());
        var extra=new DisposableRestoreFinancialDependencyPreflight.Mapping(
                Map.of(3L,103L,4L,103L),Map.of(51L,151L),
                Map.of(11L,111L),Map.of(31L,131L),Map.of(41L,141L));
        denied(ARCHIVE,extra);
    }
}
