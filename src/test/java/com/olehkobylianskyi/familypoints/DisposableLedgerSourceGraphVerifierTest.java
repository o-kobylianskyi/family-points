package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DisposableLedgerSourceGraphVerifierTest {
    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile file=new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector=
            new ProtectiveBackupImportInspector(file,codec);
    private final DisposableLedgerSourceGraphVerifier verifier=
            new DisposableLedgerSourceGraphVerifier(inspector,file,codec);

    private DisposableLedgerSourceGraphVerifier.Result verify(String extraRows,String sourceType,
            Long sourceId,long ledgerMember,long taskMember) throws Exception {
        return verifyEdited(extraRows,sourceType,sourceId,ledgerMember,taskMember,x->x);
    }
    private DisposableLedgerSourceGraphVerifier.Result verifyEdited(String extraRows,
            String sourceType,Long sourceId,long ledgerMember,long taskMember,
            java.util.function.UnaryOperator<String> edit) throws Exception {
        var pointLedger="{\"id\":100,\"member_id\":"+ledgerMember+
                ",\"point_type_id\":3,\"amount\":5,\"source_type\":\""+sourceType+"\""+
                (sourceId==null?",\"source_id\":null":",\"source_id\":"+sourceId)+
                ("TASK".equals(sourceType)?",\"type\":\"EARN\"":"")+"}";
        String body="{\"format\":\"family-points-export-v1\",\"workspaceId\":17,\"categories\":{"+
                "\"POINT_TYPES\":[{\"id\":3,\"family_id\":17,\"code\":\"POINTS\"}],"+
                "\"TASK_EXECUTIONS\":[{\"id\":42,\"member_id\":"+taskMember+
                ",\"task_definition_id\":77}],"+
                "\"TASK_DEFINITIONS\":[{\"id\":77,\"family_id\":17}],"+
                "\"POINT_LEDGER\":["+pointLedger+extraRows+"]}}";
        body=edit.apply(body);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        var encrypted=codec.seal(UUID.randomUUID(),17,UUID.randomUUID(),0,
                "a".repeat(64),"V50",body.getBytes(StandardCharsets.UTF_8),key);
        return verifier.inspect(file.encode(encrypted),17,key);
    }
    @Test void taskSourceRequiresSameMemberAndArchivedTask() throws Exception {
        var accepted=verify("","TASK",42L,4,4);
        assertEquals(1,accepted.taskRows());
        assertEquals(0,accepted.manualRows());
        assertEquals(42L,accepted.ledgerToTaskSource().get(100L));
        assertThrows(SecurityException.class,()->verify("","TASK",42L,4,5));
        assertThrows(SecurityException.class,()->verify("","TASK",99L,4,4));
        assertThrows(SecurityException.class,()->verify("","TASK",null,4,4));
    }
    @Test void taskLedgerSignedEarnAndPenaltyAreExplicitAndSpendsCannotMasquerade()
            throws Exception {
        var penalty=verifyEdited("","TASK",42L,4,4,
                body->body.replace("\"amount\":5,\"source_type\":\"TASK\"",
                        "\"amount\":-7,\"source_type\":\"TASK\"")
                        .replace("\"type\":\"EARN\"","\"type\":\"PENALTY\""));
        assertEquals(1,penalty.taskRows());
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"type\":\"EARN\"","\"type\":\"SPEND\"")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"amount\":5,\"source_type\":\"TASK\"",
                        "\"amount\":-5,\"source_type\":\"TASK\"")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"amount\":5,\"source_type\":\"TASK\"",
                        "\"amount\":0,\"source_type\":\"TASK\"")));
    }

    @Test void authenticatedTaskSnapshotAmountAndCurrencyMustMatchLedger()
            throws Exception {
        var reward=verifyEdited("","TASK",42L,4,4,
                body->body.replace("\"task_definition_id\":77}",
                        "\"task_definition_id\":77,\"reward_amount\":5,"+
                        "\"reward_point_type_id\":3}"));
        assertEquals(1,reward.taskRows());
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"task_definition_id\":77}",
                        "\"task_definition_id\":77,\"reward_amount\":6,"+
                        "\"reward_point_type_id\":3}")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"task_definition_id\":77}",
                        "\"task_definition_id\":77,\"reward_amount\":5,"+
                        "\"reward_point_type_id\":99}")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"task_definition_id\":77}",
                        "\"task_definition_id\":77,\"reward_amount\":5}")));
        var penalty=verifyEdited("","TASK",42L,4,4,
                body->body.replace("\"task_definition_id\":77}",
                        "\"task_definition_id\":77,\"penalty_amount\":7,"+
                        "\"penalty_point_type_id\":3}")
                        .replace("\"amount\":5,\"source_type\":\"TASK\"",
                                 "\"amount\":-7,\"source_type\":\"TASK\"")
                        .replace("\"type\":\"EARN\"","\"type\":\"PENALTY\""));
        assertEquals(1,penalty.taskRows());
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"task_definition_id\":77}",
                        "\"task_definition_id\":77,\"penalty_amount\":8,"+
                        "\"penalty_point_type_id\":3}")
                        .replace("\"amount\":5,\"source_type\":\"TASK\"",
                                 "\"amount\":-7,\"source_type\":\"TASK\"")
                        .replace("\"type\":\"EARN\"","\"type\":\"PENALTY\"")));
    }

    @Test void taskHistoryRejectsDuplicateRewardsAndDuplicatePenalties()
            throws Exception {
        assertThrows(SecurityException.class,()->verify(
                ",{\"id\":101,\"member_id\":4,\"point_type_id\":3,"+
                "\"amount\":5,\"type\":\"EARN\",\"source_type\":\"TASK\",\"source_id\":42}",
                "TASK",42L,4,4));
        assertThrows(SecurityException.class,()->verifyEdited(
                ",{\"id\":101,\"member_id\":4,\"point_type_id\":3,"+
                "\"amount\":-7,\"type\":\"PENALTY\",\"source_type\":\"TASK\",\"source_id\":42}",
                "TASK",42L,4,4,
                body->body.replace("\"amount\":5,\"source_type\":\"TASK\"",
                                  "\"amount\":-7,\"source_type\":\"TASK\"")
                          .replace("\"type\":\"EARN\"","\"type\":\"PENALTY\"")));
        var valid=verify(
                ",{\"id\":101,\"member_id\":4,\"point_type_id\":3,"+
                "\"amount\":-7,\"type\":\"PENALTY\",\"source_type\":\"TASK\",\"source_id\":42}",
                "TASK",42L,4,4);
        assertEquals(2,valid.taskRows());
    }

    @Test void manualSourceMustNotCarrySourceIdAndUnknownTypesRemainBlocked()
            throws Exception {
        var accepted=verify("","MANUAL",null,4,4);
        assertEquals(1,accepted.manualRows());
        assertEquals(0,accepted.taskRows());
        assertThrows(SecurityException.class,()->verify("","MANUAL",42L,4,4));
        assertThrows(SecurityException.class,()->verify("","REWARD_PURCHASE",42L,4,4));
        assertThrows(SecurityException.class,()->verify("","REFUND",42L,4,4));
    }
    @Test void refundAndExchangeCannotBeSmuggledIntoGenericSourceReplay()
            throws Exception {
        assertThrows(SecurityException.class,()->verifyEdited(
                "","REFUND",51L,4,4,
                body->body.replace("\"amount\":5",
                        "\"amount\":5,\"type\":\"REFUND\"")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","POINT_EXCHANGE",42L,4,4,
                body->body.replace("\"amount\":5",
                        "\"amount\":-5,\"type\":\"SPEND\"")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","POINT_EXCHANGE",42L,4,4,
                body->body.replace("\"amount\":5",
                        "\"amount\":5,\"type\":\"EARN\"")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","UNKNOWN_LEGACY",42L,4,4,
                body->body.replace("\"amount\":5",
                        "\"amount\":5,\"type\":\"EARN\"")));
    }

    @Test void authenticatedArchiveNeverTreatsMalformedCurrencyOrParentAsEmpty()
            throws Exception {
        assertThrows(IllegalArgumentException.class,()->verifyEdited(
                "","MANUAL",null,4,4,
                body->body.replace("\"POINT_TYPES\":[{\"id\":3,\"family_id\":17,\"code\":\"POINTS\"}]",
                        "\"POINT_TYPES\":null")));
        assertThrows(IllegalArgumentException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"TASK_DEFINITIONS\":[{\"id\":77,\"family_id\":17}]",
                        "\"TASK_DEFINITIONS\":null")));
        assertThrows(IllegalArgumentException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"TASK_EXECUTIONS\":[{\"id\":42,\"member_id\":4,\"task_definition_id\":77}]",
                        "\"TASK_EXECUTIONS\":null")));
    }

    @Test void unusedArchivedTaskCannotConcealIncompleteHistoricalEconomics()
            throws Exception {
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"TASK_EXECUTIONS\":[",
                        "\"TASK_EXECUTIONS\":[{\"id\":99,\"member_id\":4,"+
                        "\"task_definition_id\":77,\"reward_amount\":10},")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"TASK_EXECUTIONS\":[",
                        "\"TASK_EXECUTIONS\":[{\"id\":99,\"member_id\":4,"+
                        "\"task_definition_id\":77,\"penalty_point_type_id\":3},")));
        var accepted=verifyEdited("","TASK",42L,4,4,
                body->body.replace("\"TASK_EXECUTIONS\":[",
                        "\"TASK_EXECUTIONS\":[{\"id\":99,\"member_id\":4,"+
                        "\"task_definition_id\":77,\"reward_amount\":10,"+
                        "\"reward_point_type_id\":3},"));
        assertEquals(1,accepted.taskRows());
    }

    @Test void evenUnusedArchivedTaskCannotHaveInvalidMemberId()
            throws Exception {
        var valid=verify("","TASK",42L,4,4);
        assertEquals(1,valid.taskRows());
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"TASK_EXECUTIONS\":[",
                        "\"TASK_EXECUTIONS\":[{\"id\":43,\"member_id\":0,"+
                        "\"task_definition_id\":77},")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                body->body.replace("\"TASK_EXECUTIONS\":[",
                        "\"TASK_EXECUTIONS\":[{\"id\":44,\"member_id\":-1,"+
                        "\"task_definition_id\":77},")));
    }

    @Test void taskDefinitionMustBePresentInSameFamilyAndAmountsAreBounded()
            throws Exception {
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                s->s.replace("\"task_definition_id\":77","\"task_definition_id\":999")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                s->s.replace("\"id\":77,\"family_id\":17",
                        "\"id\":77,\"family_id\":18")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                s->s.replace("\"amount\":5","\"amount\":2147483648")));
    }

    private DisposableLedgerSourceGraphVerifier.Result purchaseProof(
            long purchaseMember,long price,long currency,long ledgerAmount,
            String operationType) throws Exception {
        return verifyEdited("","REWARD_PURCHASE",51L,4,4,
                body->body.replace("\"amount\":5",
                            "\"amount\":"+ledgerAmount+",\"type\":\""+operationType+"\"")
                        .replace("\"POINT_LEDGER\":",
                            "\"REWARD_PURCHASES\":[{\"id\":51,\"member_id\":"
                            +purchaseMember+",\"point_type_id\":"+currency
                            +",\"price_amount\":"+price+"}],\"POINT_LEDGER\":"));
    }

    @Test void purchaseSpendMustMatchArchivedPurchaseOwnerCurrencyAndPrice()
            throws Exception {
        var valid=purchaseProof(4,5,3,-5,"SPEND");
        assertEquals(1,valid.purchaseRows());
        assertEquals(51L,valid.ledgerToPurchaseSource().get(100L));
        assertThrows(SecurityException.class,()->purchaseProof(5,5,3,-5,"SPEND"));
        assertThrows(SecurityException.class,()->purchaseProof(4,5,9,-5,"SPEND"));
        assertThrows(SecurityException.class,()->purchaseProof(4,6,3,-5,"SPEND"));
        assertThrows(SecurityException.class,()->purchaseProof(4,5,3,5,"SPEND"));
        assertThrows(SecurityException.class,()->purchaseProof(4,5,3,-5,"EARN"));
    }

    @Test void unusedArchivedPurchaseStillRequiresValidCurrencyOwnerAndPrice()
            throws Exception {
        var accepted=purchaseProof(4,5,3,-5,"SPEND");
        assertEquals(1,accepted.purchaseRows());
        assertThrows(SecurityException.class,()->purchaseProof(0,5,3,-5,"SPEND"));
        assertThrows(SecurityException.class,()->purchaseProof(4,0,3,-5,"SPEND"));
        assertThrows(SecurityException.class,()->purchaseProof(4,2147483648L,3,-5,"SPEND"));
        // The second archived purchase has no ledger row. Its foreign point
        // type still corrupts authenticated source graph closure.
        assertThrows(SecurityException.class,()->verifyEdited(
                "","REWARD_PURCHASE",51L,4,4,
                body->body.replace("\"amount\":5",
                        "\"amount\":-5,\"type\":\"SPEND\"")
                        .replace("\"POINT_LEDGER\":",
                        "\"REWARD_PURCHASES\":[{\"id\":51,\"member_id\":4,"+
                        "\"point_type_id\":3,\"price_amount\":5},"+
                        "{\"id\":52,\"member_id\":4,\"point_type_id\":999,"+
                        "\"price_amount\":5}],\"POINT_LEDGER\":")));
    }

    @Test void sourceLedgerBalanceInventoryIsPartitionedAndAuditable()
            throws Exception {
        var accepted=verifyEdited(
                ",{\"id\":101,\"member_id\":5,\"point_type_id\":3,"+
                "\"amount\":-3,\"source_type\":\"MANUAL\",\"source_id\":null},"+
                "{\"id\":102,\"member_id\":4,\"point_type_id\":3,"+
                "\"amount\":-2,\"source_type\":\"MANUAL\",\"source_id\":null}",
                "MANUAL",null,4,4,body->body);
        assertEquals(3,accepted.ledgerRows());
        assertEquals(3L,accepted.signedMemberCurrencyBalances().get("4:3"));
        assertEquals(-3L,accepted.signedMemberCurrencyBalances().get("5:3"));
        assertEquals(2,accepted.signedMemberCurrencyBalances().size());
    }

    @Test void mixedTaskPurchaseAndManualLedgerConservesEachMemberCurrency()
            throws Exception {
        var accepted=verifyEdited(
                ",{\"id\":101,\"member_id\":4,\"point_type_id\":3,"+
                "\"amount\":-4,\"type\":\"SPEND\",\"source_type\":\"REWARD_PURCHASE\",\"source_id\":51},"+
                "{\"id\":102,\"member_id\":5,\"point_type_id\":3,"+
                "\"amount\":7,\"source_type\":\"MANUAL\",\"source_id\":null}",
                "TASK",42L,4,4,
                body->body.replace("\"POINT_LEDGER\":",
                        "\"REWARD_PURCHASES\":[{\"id\":51,\"member_id\":4,"+
                        "\"point_type_id\":3,\"price_amount\":4}],\"POINT_LEDGER\":"));
        assertEquals(1,accepted.taskRows());
        assertEquals(1,accepted.purchaseRows());
        assertEquals(1,accepted.manualRows());
        assertEquals(3,accepted.ledgerRows());
        assertEquals(1L,accepted.signedMemberCurrencyBalances().get("4:3"));
        assertEquals(7L,accepted.signedMemberCurrencyBalances().get("5:3"));
        assertEquals(42L,accepted.ledgerToTaskSource().get(100L));
        assertEquals(51L,accepted.ledgerToPurchaseSource().get(101L));
    }

    @Test void archiveRejectsDuplicateCurrencyCodesAndInvalidCurrencyId()
            throws Exception {
        assertThrows(SecurityException.class,()->verifyEdited(
                "","MANUAL",null,4,4,
                body->body.replace("\"POINT_TYPES\":[{\"id\":3,\"family_id\":17,\"code\":\"POINTS\"}]",
                        "\"POINT_TYPES\":[{\"id\":3,\"family_id\":17,\"code\":\"POINTS\"},"+
                        "{\"id\":8,\"family_id\":17,\"code\":\"POINTS\"}]")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","MANUAL",null,4,4,
                body->body.replace("\"id\":3,\"family_id\":17,\"code\":\"POINTS\"",
                        "\"id\":0,\"family_id\":17,\"code\":\"POINTS\"")));
    }

    @Test void duplicateChargeOfSamePurchaseFailsClosed() throws Exception {
        assertThrows(SecurityException.class,()->verifyEdited(
                "","REWARD_PURCHASE",51L,4,4,
                body->body.replace("\"amount\":5",
                            "\"amount\":-5,\"type\":\"SPEND\"")
                        .replace("\"POINT_LEDGER\":",
                            "\"REWARD_PURCHASES\":[{\"id\":51,\"member_id\":4,"
                            +"\"point_type_id\":3,\"price_amount\":5}],\"POINT_LEDGER\":")
                        .replace("\"POINT_LEDGER\":[",
                            "\"POINT_LEDGER\":[{\"id\":101,\"member_id\":4,"
                            +"\"point_type_id\":3,\"amount\":-5,\"type\":\"SPEND\","
                            +"\"source_type\":\"REWARD_PURCHASE\",\"source_id\":51},")
        ));
    }

}
