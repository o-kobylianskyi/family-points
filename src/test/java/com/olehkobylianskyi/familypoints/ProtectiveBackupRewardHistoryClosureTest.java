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
    @Test void requirementArchiveEnforcesParentGraphAndFamilyScope() throws Exception {
        var valid=inspect("""
                {
                  "REWARD_DEFINITIONS":[{"id":4,"family_id":17}],
                  "REWARD_REQUIREMENTS":[{
                    "id":5,"family_id":17,"reward_definition_id":4,
                    "reward_request_id":null,"task_definition_id":null
                  }]
                }
                """);
        assertEquals(1,valid.counts().get("REWARD_REQUIREMENTS").intValue());
        assertFalse(valid.blockers().contains("MISSING_CATEGORY:REWARD_DEFINITIONS"));
        assertFalse(valid.restorable());

        var missingParent=inspect("""
                {
                  "REWARD_REQUIREMENTS":[{
                    "id":5,"family_id":17,"reward_definition_id":4,
                    "reward_request_id":null,"task_definition_id":null
                  }]
                }
                """);
        assertTrue(missingParent.blockers().contains("MISSING_CATEGORY:REWARD_DEFINITIONS"));

        var invalidOwner=inspect("""
                {
                  "REWARD_REQUIREMENTS":[{
                    "id":5,"family_id":17,"reward_definition_id":null,
                    "reward_request_id":null,"task_definition_id":null
                  }]
                }
                """);
        assertTrue(invalidOwner.blockers().contains("INVALID_REWARD_REQUIREMENT_OWNER"));

        assertThrows(SecurityException.class,()->inspect("""
                {
                  "REWARD_REQUIREMENTS":[{
                    "id":5,"family_id":18,"reward_definition_id":4,
                    "reward_request_id":null
                  }]
                }
                """));
    }

    @Test void rewardCategoryCompositeKeysAndParentGraphAreExplicitlyVerified() throws Exception {
        var good=inspect("""
                {
                  "REWARD_DEFINITIONS":[{"id":9,"family_id":17}],
                  "REWARD_CATEGORIES":[{"id":6,"family_id":17}],
                  "REWARD_DEFINITION_CATEGORIES":[{
                    "reward_definition_id":9,"category_id":6
                  }]
                }
                """);
        assertEquals(1,good.counts().get("REWARD_DEFINITION_CATEGORIES").intValue());
        assertFalse(good.blockers().contains("MISSING_CATEGORY:REWARD_CATEGORIES"));
        assertFalse(good.blockers().contains("MISSING_CATEGORY:REWARD_DEFINITIONS"));
        assertFalse(good.restorable());

        var orphan=inspect("""
                {
                  "REWARD_DEFINITIONS":[{"id":9,"family_id":17}],
                  "REWARD_DEFINITION_CATEGORIES":[{
                    "reward_definition_id":9,"category_id":6
                  }]
                }
                """);
        assertTrue(orphan.blockers().contains("MISSING_CATEGORY:REWARD_CATEGORIES"));

        assertThrows(IllegalArgumentException.class,()->inspect("""
                {
                  "REWARD_DEFINITIONS":[{"id":9,"family_id":17}],
                  "REWARD_CATEGORIES":[{"id":6,"family_id":17}],
                  "REWARD_DEFINITION_CATEGORIES":[
                    {"reward_definition_id":9,"category_id":6},
                    {"reward_definition_id":9,"category_id":6}
                  ]
                }
                """));
        assertThrows(SecurityException.class,()->inspect("""
                {
                  "REWARD_DEFINITIONS":[{"id":9,"family_id":17}],
                  "REWARD_CATEGORIES":[{"id":6,"family_id":18}],
                  "REWARD_DEFINITION_CATEGORIES":[{
                    "reward_definition_id":9,"category_id":6
                  }]
                }
                """));
    }

    @Test void obligationCompositeBlocksRequireBothParentsAndRejectDuplicates() throws Exception {
        var valid=inspect("""
                {
                  "REWARD_DEFINITIONS":[{"id":9,"family_id":17}],
                  "REWARD_CATEGORIES":[{"id":6,"family_id":17}],
                  "REWARD_PURCHASES":[{
                    "id":21,"reward_definition_id":9,"member_id":4,"point_type_id":3
                  }],
                  "REWARD_OBLIGATIONS":[{
                    "id":31,"family_id":17,"member_id":4,
                    "reward_purchase_id":21,"requirement_id":null
                  }],
                  "REWARD_OBLIGATION_BLOCKED_CATEGORIES":[{
                    "obligation_id":31,"category_id":6
                  }],
                  "REWARD_OBLIGATION_BLOCKED_REWARDS":[{
                    "obligation_id":31,"reward_definition_id":9
                  }]
                }
                """);
        assertEquals(1,valid.counts().get("REWARD_OBLIGATION_BLOCKED_CATEGORIES").intValue());
        assertEquals(1,valid.counts().get("REWARD_OBLIGATION_BLOCKED_REWARDS").intValue());
        assertFalse(valid.blockers().contains("MISSING_CATEGORY:REWARD_OBLIGATIONS"));
        assertFalse(valid.blockers().contains("DANGLING_REFERENCE:REWARD_OBLIGATION_BLOCKED_REWARDS.reward_definition_id"));
        assertFalse(valid.restorable());
        var orphan=inspect("""
                {
                  "REWARD_OBLIGATION_BLOCKED_CATEGORIES":[{
                    "obligation_id":31,"category_id":6
                  }]
                }
                """);
        assertTrue(orphan.blockers().contains("MISSING_CATEGORY:REWARD_OBLIGATIONS"));
        assertTrue(orphan.blockers().contains("MISSING_CATEGORY:REWARD_CATEGORIES"));
        assertThrows(IllegalArgumentException.class,()->inspect("""
                {
                  "REWARD_OBLIGATION_BLOCKED_REWARDS":[
                    {"obligation_id":31,"reward_definition_id":9},
                    {"obligation_id":31,"reward_definition_id":9}
                  ]
                }
                """));
        assertThrows(SecurityException.class,()->inspect("""
                {
                  "REWARD_OBLIGATIONS":[{
                    "id":31,"family_id":18,"member_id":4,"reward_purchase_id":21
                  }]
                }
                """));
    }

    @Test void pointTypeCatalogMustBindFinanceFksAndHaveUniqueTenantCodes() throws Exception {
        var valid=inspect("""
                {
                  "POINT_TYPES":[{"id":3,"family_id":17,"code":"POINTS",
                                  "name":"Points","active":true}],
                  "POINT_LEDGER":[{"id":9,"member_id":4,"point_type_id":3,
                                   "amount":8,"type":"EARN","source_type":"MANUAL"}]
                }
                """);
        assertEquals(1,valid.counts().get("POINT_TYPES").intValue());
        assertFalse(valid.blockers().contains("MISSING_CATEGORY:POINT_TYPES"));
        assertFalse(valid.blockers().contains("DANGLING_REFERENCE:POINT_LEDGER.point_type_id"));
        assertFalse(valid.restorable());

        var dangling=inspect("""
                {
                  "POINT_TYPES":[{"id":3,"family_id":17,"code":"POINTS"}],
                  "POINT_LEDGER":[{"id":9,"member_id":4,"point_type_id":5,
                                   "amount":8,"type":"EARN","source_type":"MANUAL"}]
                }
                """);
        assertTrue(dangling.blockers().contains(
                "DANGLING_REFERENCE:POINT_LEDGER.point_type_id"));

        var missing=inspect("""
                {
                  "POINT_LEDGER":[{"id":9,"member_id":4,"point_type_id":3,
                                   "amount":8,"type":"EARN","source_type":"MANUAL"}]
                }
                """);
        assertTrue(missing.blockers().contains("MISSING_CATEGORY:POINT_TYPES"));

        var duplicateCodes=inspect("""
                {
                  "POINT_TYPES":[
                    {"id":3,"family_id":17,"code":"POINTS"},
                    {"id":4,"family_id":17,"code":"POINTS"}
                  ]
                }
                """);
        assertTrue(duplicateCodes.blockers().contains("INVALID_POINT_TYPE_CODE"));
        assertThrows(SecurityException.class,()->inspect("""
                {
                  "POINT_TYPES":[{"id":3,"family_id":18,"code":"POINTS"}]
                }
                """));
    }

}
