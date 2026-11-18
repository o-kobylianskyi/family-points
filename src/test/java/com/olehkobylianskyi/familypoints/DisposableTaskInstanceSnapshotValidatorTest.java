package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Backend B B-B-06 test-only original-history validation. */
class DisposableTaskInstanceSnapshotValidatorTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String HISTORIC="""
        {
          "id":21,"task_definition_id":11,"member_id":3,
          "original_assigned_member_id":3,"scheduled_date":"2026-11-18",
          "slot_key":"morning","title":"Original task",
          "description":"Original description","mandatory":true,
          "due_time":"12:30:00","status":"COMPLETED",
          "claimed_at":"2026-11-18T08:50:00",
          "started_at":"2026-11-18T09:00:00",
          "completed_at":"2026-11-18T09:15:00",
          "excuse_reason":null,"excuse_comment":null,
          "reward_amount":5,"reward_point_type_id":51,
          "penalty_amount":null,"penalty_point_type_id":null,
          "reward_reputation_amount":null,"penalty_reputation_amount":null,
          "reward_processed":true,"penalty_processed":false
        }
        """;
    private static ObjectNode snapshot() throws Exception {
        return (ObjectNode)JSON.readTree(HISTORIC);
    }
    private static DisposableTaskInstanceSnapshotValidator.Snapshot parse(JsonNode row) {
        return DisposableTaskInstanceSnapshotValidator.parse(
                row,Map.of(3L,103L),Map.of(11L,1011L),Map.of(51L,1051L));
    }
    private static JsonNode taskLedger(int amount,int currency,String kind) throws Exception {
        return JSON.readTree("[{\"source_type\":\"TASK\",\"source_id\":21,"+
                "\"member_id\":3,\"point_type_id\":"+currency+","+
                "\"type\":\""+kind+"\",\"amount\":"+amount+"}]");
    }

    @Test void preservesCompleteHistoricalFieldsRatherThanRebuildingFromDefinition()
            throws Exception {
        var s=parse(snapshot());
        assertEquals("COMPLETED",s.status());
        assertEquals("morning",s.slotKey());
        assertEquals("Original task",s.title());
        assertEquals(5,s.rewardAmount());
        assertEquals(51L,s.rewardPointType());
        assertTrue(s.rewardProcessed());
        assertEquals(java.time.LocalDate.parse("2026-11-18"),s.scheduledDate());
        assertEquals(java.time.LocalTime.parse("12:30:00"),s.dueTime());
        assertEquals(3L,s.originalAssignedMember());
        DisposableTaskInstanceSnapshotValidator.verifyLedgerEconomy(
                taskLedger(5,51,"EARN"),Map.of(21L,s));
    }

    @Test void missingAndInvalidHistoricalMetadataDeny() throws Exception {
        for(String removed:List.of("scheduled_date","slot_key","title",
                "mandatory","status","reward_processed","penalty_processed")) {
            var node=snapshot();node.remove(removed);
            assertThrows(SecurityException.class,()->parse(node),removed);
        }
        var wrongDate=snapshot();wrongDate.put("scheduled_date","2026-15-99");
        assertThrows(SecurityException.class,()->parse(wrongDate));
        var wrongSlot=snapshot();wrongSlot.put("slot_key","invalid space");
        assertThrows(SecurityException.class,()->parse(wrongSlot));
        var badStatus=snapshot();badStatus.put("status","UNKNOWN");
        assertThrows(SecurityException.class,()->parse(badStatus));
        var badTime=snapshot();badTime.put("completed_at","2026-11-18T08:59:59");
        assertThrows(SecurityException.class,()->parse(badTime));
    }

    @Test void originalAssigneeAndCurrencyMustBeInApprovedMapping() throws Exception {
        var wrongAssignee=snapshot();wrongAssignee.put("original_assigned_member_id",999);
        assertThrows(SecurityException.class,()->parse(wrongAssignee));
        var wrongCurrency=snapshot();wrongCurrency.put("reward_point_type_id",999);
        assertThrows(SecurityException.class,()->parse(wrongCurrency));
        var incomplete=snapshot();incomplete.remove("reward_point_type_id");
        assertThrows(SecurityException.class,()->parse(incomplete));
        var missingReward=snapshot();missingReward.remove("reward_amount");
        assertThrows(SecurityException.class,()->parse(missingReward));
    }

    @Test void ledgerMustMatchOriginalSnapshotAmountCurrencyAndProcessingState()
            throws Exception {
        var original=parse(snapshot());
        for(var bad:List.of(
                taskLedger(4,51,"EARN"),
                taskLedger(5,999,"EARN"),
                taskLedger(-5,51,"PENALTY")))
            assertThrows(SecurityException.class,()->
                    DisposableTaskInstanceSnapshotValidator.verifyLedgerEconomy(
                            bad,Map.of(21L,original)));
        var noPay=snapshot();noPay.put("reward_processed",false);
        assertThrows(SecurityException.class,()->
                DisposableTaskInstanceSnapshotValidator.verifyLedgerEconomy(
                        taskLedger(5,51,"EARN"),Map.of(21L,parse(noPay))));
        assertThrows(SecurityException.class,()->
                DisposableTaskInstanceSnapshotValidator.verifyLedgerEconomy(
                        JSON.readTree("[]"),Map.of(21L,original)));
    }

    @Test void reputationOnlyProcessedRewardDoesNotInventPointLedger() throws Exception {
        var snap=snapshot();
        snap.putNull("reward_amount");
        snap.putNull("reward_point_type_id");
        snap.put("reward_reputation_amount",7);
        var validated=parse(snap);
        DisposableTaskInstanceSnapshotValidator.verifyLedgerEconomy(
                JSON.readTree("[]"),Map.of(21L,validated));
        assertEquals(7,validated.rewardReputationAmount());
    }

    @Test void explicitNullAndExcuseHistoryCannotBeSilentlyDiscarded() throws Exception {
        var missing=snapshot();
        missing.remove("excuse_comment");
        assertThrows(SecurityException.class,()->parse(missing));
        var invalid=snapshot();
        invalid.put("excuse_reason","NOT_A_REASON");
        assertThrows(SecurityException.class,()->parse(invalid));
        var excused=snapshot();
        excused.put("status","EXCUSED");
        excused.putNull("excuse_reason");
        assertThrows(SecurityException.class,()->parse(excused));
        excused.put("excuse_reason","ILLNESS");
        excused.put("excuse_comment","Reviewed by family");
        var preserved=parse(excused);
        assertEquals("ILLNESS",preserved.excuseReason());
        assertEquals("Reviewed by family",preserved.excuseComment());
    }

    @Test void duplicateTaskLedgerPayoutRejectedBeforeImport() throws Exception {
        var s=parse(snapshot());
        var repeated=JSON.readTree("[{\"source_type\":\"TASK\",\"source_id\":21,"+
                "\"member_id\":3,\"point_type_id\":51,\"amount\":5,\"type\":\"EARN\"},"+
                "{\"source_type\":\"TASK\",\"source_id\":21,"+
                "\"member_id\":3,\"point_type_id\":51,\"amount\":5,\"type\":\"EARN\"}]");
        assertThrows(SecurityException.class,()->
                DisposableTaskInstanceSnapshotValidator.verifyLedgerEconomy(
                        repeated,Map.of(21L,s)));
    }
}
