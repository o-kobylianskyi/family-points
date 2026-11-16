package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Authenticated JSON fixtures expose logical nullable FK and historical
 * identity gaps without connecting to or changing a database. */
class ProtectiveBackupRewardHistoryClosureTest {
    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile portable=new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector=
            new ProtectiveBackupImportInspector(portable,codec);

    private ProtectiveBackupImportInspector.Inspection inspect(String categories)
            throws Exception {
        long family=17L;
        String plain="{\"format\":\"family-points-export-v1\",\"workspaceId\":17,"
                +"\"categories\":"+categories+"}";
        var keys=KeyGenerator.getInstance("AES");
        keys.init(256);
        SecretKey key=keys.generateKey();
        var sealed=codec.seal(UUID.randomUUID(),family,UUID.randomUUID(),0L,
                "a".repeat(64),"V50",plain.getBytes(StandardCharsets.UTF_8),key);
        return inspector.inspect(portable.encode(sealed),family,key);
    }

    @Test void requestOriginPurchaseWithoutDefinitionRequiresOnlyRequestAndIdentityRemaps()
            throws Exception {
        var report=inspect("""
                {
                  "REWARD_REQUESTS":[{
                    "id":13,"family_id":17,"requested_by_member_id":7,
                    "reward_definition_id":null,"approved_point_type_id":3
                  }],
                  "REWARD_PURCHASES":[{
                    "id":21,"member_id":7,"reward_definition_id":null,
                    "reward_request_id":13,"point_type_id":3
                  }]
                }
                """);
        assertFalse(report.restorable());
        assertFalse(report.blockers().contains("INVALID_REFERENCE:REWARD_PURCHASES.reward_definition_id"));
        assertFalse(report.blockers().contains("MISSING_CATEGORY:REWARD_DEFINITIONS"));
        assertFalse(report.blockers().contains("MISSING_CATEGORY:REWARD_REQUESTS"));
        assertTrue(report.blockers().contains(
                "CURRENCY_REBIND_REQUIRED:REWARD_PURCHASES.point_type_id"));
        assertTrue(report.blockers().contains(
                "IDENTITY_REBIND_REQUIRED:REWARD_REQUESTS.requested_by_member_id"));
    }

    @Test void unarchivedRequestedRewardAndMalformedCurrencyStayBlocked()
            throws Exception {
        var report=inspect("""
                {
                  "REWARD_PURCHASES":[{
                    "id":22,"member_id":7,"reward_definition_id":null,
                    "reward_request_id":13,"point_type_id":-4
                  }]
                }
                """);
        assertTrue(report.blockers().contains("MISSING_CATEGORY:REWARD_REQUESTS"));
        assertTrue(report.blockers().contains(
                "INVALID_REFERENCE:REWARD_PURCHASES.point_type_id"));
        assertFalse(report.restorable());
    }

    @Test void invalidDefinitionFkRemainsBlockedWhenActuallyPresent() throws Exception {
        var report=inspect("""
                {
                  "REWARD_REQUESTS":[{
                    "id":33,"family_id":17,"requested_by_member_id":7,
                    "reward_definition_id":100,"approved_point_type_id":null
                  }]
                }
                """);
        assertTrue(report.blockers().contains("MISSING_CATEGORY:REWARD_DEFINITIONS"));
        assertFalse(report.restorable());
    }
}
