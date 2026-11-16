package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static org.junit.jupiter.api.Assertions.*;

/** Offline-only signed source history fixtures. No Restore or database writes. */
class ProtectiveBackupExchangeSourceGraphTest {
    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile file=new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector=
            new ProtectiveBackupImportInspector(file,codec);
    private final ObjectMapper mapper=new ObjectMapper();

    private ProtectiveBackupImportInspector.Inspection inspect(UnaryOperator<ObjectNode> edit)
            throws Exception {
        String original="""
                {"format":"family-points-export-v1","workspaceId":17,"categories":{
                  "POINT_TYPES":[
                    {"id":3,"family_id":17,"code":"POINTS","active":true},
                    {"id":4,"family_id":17,"code":"TOKENS","active":true}],
                  "POINT_EXCHANGE_RATES":[{
                    "id":20,"family_id":17,"from_point_type_id":3,
                    "to_point_type_id":4,"from_amount":10,"to_amount":2,
                    "approval_mode":"AUTO","active":true}],
                  "POINT_EXCHANGE_REQUESTS":[{
                    "id":30,"member_id":7,"exchange_rate_id":20,
                    "from_point_type_id":3,"to_point_type_id":4,
                    "rate_from_amount":10,"rate_to_amount":2,
                    "from_amount":10,"to_amount":2,"status":"APPROVED"}],
                  "POINT_LEDGER":[
                    {"id":41,"member_id":7,"point_type_id":3,"amount":-10,
                     "source_type":"POINT_EXCHANGE","source_id":30,"type":"SPEND"},
                    {"id":42,"member_id":7,"point_type_id":4,"amount":2,
                     "source_type":"POINT_EXCHANGE","source_id":30,"type":"EARN"}]
                }}
                """;
        var root=edit.apply((ObjectNode)mapper.readTree(original));
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        var artifact=codec.seal(UUID.randomUUID(),17,UUID.randomUUID(),0L,
                "a".repeat(64),"V50",mapper.writeValueAsBytes(root),key);
        return inspector.inspect(file.encode(artifact),17,key);
    }
    private static ObjectNode categories(ObjectNode root) {
        return (ObjectNode)root.path("categories");
    }
    private static ObjectNode first(ObjectNode root,String kind) {
        return (ObjectNode)root.path("categories").path(kind).get(0);
    }
    private static ObjectNode second(ObjectNode root,String kind) {
        return (ObjectNode)root.path("categories").path(kind).get(1);
    }

    @Test void signedCompletePairIsSourceConsistentButNeverRestorable() throws Exception {
        var result=inspect(x->x);
        assertEquals(1,result.counts().get("POINT_EXCHANGE_REQUESTS").intValue());
        assertEquals(1,result.counts().get("POINT_EXCHANGE_RATES").intValue());
        assertEquals(2,result.counts().get("POINT_LEDGER").intValue());
        assertFalse(result.blockers().contains("INVALID_EXCHANGE_LEDGER_PAIR"));
        assertFalse(result.blockers().contains("EXCHANGE_LEDGER_ECONOMICS_MISMATCH"));
        assertFalse(result.blockers().contains("EXCHANGE_REQUEST_RATE_CURRENCY_MISMATCH"));
        assertFalse(result.restorable());
    }

    @Test void omittedRateOrMismatchedCurrencyFailsClosed() throws Exception {
        var withoutRate=inspect(root->{
            categories(root).remove("POINT_EXCHANGE_RATES");
            return root;
        });
        assertTrue(withoutRate.blockers().contains("MISSING_CATEGORY:POINT_EXCHANGE_RATES"));
        var wrongRate=inspect(root->{
            first(root,"POINT_EXCHANGE_REQUESTS").put("from_point_type_id",4);
            return root;
        });
        assertTrue(wrongRate.blockers().contains("EXCHANGE_REQUEST_RATE_CURRENCY_MISMATCH"));
    }

    @Test void incorrectAmountStatusOrMemberFailsClosed() throws Exception {
        var wrongAmount=inspect(root->{
            second(root,"POINT_LEDGER").put("amount",3);
            return root;
        });
        assertTrue(wrongAmount.blockers().contains("EXCHANGE_LEDGER_ECONOMICS_MISMATCH"));
        var pending=inspect(root->{
            first(root,"POINT_EXCHANGE_REQUESTS").put("status","PENDING");
            return root;
        });
        assertTrue(pending.blockers().contains("INVALID_EXCHANGE_LEDGER_PAIR"));
        var differentOwner=inspect(root->{
            second(root,"POINT_LEDGER").put("member_id",8);
            return root;
        });
        assertTrue(differentOwner.blockers().contains("EXCHANGE_LEDGER_OWNER_MISMATCH"));
    }

    @Test void missingAndExtraLegsFailClosed() throws Exception {
        var missing=inspect(root->{
            ((ArrayNode)categories(root).path("POINT_LEDGER")).remove(1);
            return root;
        });
        assertTrue(missing.blockers().contains("INVALID_EXCHANGE_LEDGER_PAIR"));
        var duplicate=inspect(root->{
            var rows=(ArrayNode)categories(root).path("POINT_LEDGER");
            ObjectNode duplicateRow=second(root,"POINT_LEDGER").deepCopy();
            duplicateRow.put("id",43);
            rows.add(duplicateRow);
            return root;
        });
        assertTrue(duplicate.blockers().contains("INVALID_EXCHANGE_LEDGER_PAIR"));
    }

    @Test void foreignFamilyRateCannotBeSmuggledInSignedArchive() throws Exception {
        assertThrows(SecurityException.class,()->inspect(root->{
            first(root,"POINT_EXCHANGE_RATES").put("family_id",18);
            return root;
        }));
    }
}
