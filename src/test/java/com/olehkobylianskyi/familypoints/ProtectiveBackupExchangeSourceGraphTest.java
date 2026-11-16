package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AES-authenticated portable exchange history preflight, source graph only.
 * No target replay, mutation or production VERIFIED proof.
 */
class ProtectiveBackupExchangeSourceGraphTest {
    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile file=new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector=
            new ProtectiveBackupImportInspector(file,codec);

    private ProtectiveBackupImportInspector.Inspection inspect(UnaryOperator<String> edit)
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
        String json=edit.apply(original);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        var artifact=codec.seal(UUID.randomUUID(),17,UUID.randomUUID(),0L,
                "a".repeat(64),"V50",json.getBytes(StandardCharsets.UTF_8),key);
        return inspector.inspect(file.encode(artifact),17,key);
    }

    @Test void signedCompletePairHasExactExchangeGraphButNeverRestorable() throws Exception {
        var result=inspect(s->s);
        assertEquals(1,result.counts().get("POINT_EXCHANGE_REQUESTS").intValue());
        assertEquals(1,result.counts().get("POINT_EXCHANGE_RATES").intValue());
        assertFalse(result.blockers().contains("INVALID_EXCHANGE_LEDGER_PAIR"));
        assertFalse(result.blockers().contains("EXCHANGE_LEDGER_ECONOMICS_MISMATCH"));
        assertFalse(result.blockers().contains("EXCHANGE_REQUEST_RATE_CURRENCY_MISMATCH"));
        assertFalse(result.restorable());
    }

    @Test void missingRateOrChangedCurrencyMustBlock() throws Exception {
        var withoutRates=inspect(s->s.replace(
                "\"POINT_EXCHANGE_RATES\":[{\"id\":20,\"family_id\":17,\"from_point_type_id\":3,\"to_point_type_id\":4,\"from_amount\":10,\"to_amount\":2,\"approval_mode\":\"AUTO\",\"active\":true}],",
                ""));
        assertTrue(withoutRates.blockers().contains("MISSING_CATEGORY:POINT_EXCHANGE_RATES"));
        var badCurrency=inspect(s->s.replace(
                "\"exchange_rate_id\":20,\n                    \"from_point_type_id\":3,\"to_point_type_id\":4",
                "\"exchange_rate_id\":20,\n                    \"from_point_type_id\":4,\"to_point_type_id\":3"));
        assertTrue(badCurrency.blockers().contains("EXCHANGE_REQUEST_RATE_CURRENCY_MISMATCH"));
    }

    @Test void exchangeRejectsIncompleteDuplicateOrMismatchedLedgerLegs() throws Exception {
        var incomplete=inspect(s->s.replace(
                "\"id\":42,\"member_id\":7,\"point_type_id\":4,\"amount\":2,",
                "\"id\":42,\"member_id\":7,\"point_type_id\":4,\"amount\":3,"));
        assertTrue(incomplete.blockers().contains("EXCHANGE_LEDGER_ECONOMICS_MISMATCH"));
        var pending=inspect(s->s.replace("\"status\":\"APPROVED\"",
                "\"status\":\"PENDING\""));
        assertTrue(pending.blockers().contains("INVALID_EXCHANGE_LEDGER_PAIR"));
        var wrongMember=inspect(s->s.replace(
                "\"id\":42,\"member_id\":7", "\"id\":42,\"member_id\":8"));
        assertTrue(wrongMember.blockers().contains("EXCHANGE_LEDGER_OWNER_MISMATCH"));
    }

    @Test void foreignFamilyRateIsNotTrustedEvenInAuthenticatedArchive() throws Exception {
        assertThrows(SecurityException.class,()->inspect(s->s.replace(
                "\"id\":20,\"family_id\":17", "\"id\":20,\"family_id\":18")));
    }
}
