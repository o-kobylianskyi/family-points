package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.PortableMemberAncestryV2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QA-074 red-first regression for mandatory original-family provenance.
 * Isolated QA branch: expected to FAIL against the current permissive
 * history schema, without modifying production code or main.
 */
class PortableV2HistoryTenantProvenanceQaTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long SOURCE_WORKSPACE = 17;
    private static final long SOURCE_EPOCH = 7;
    private static final UUID SNAPSHOT =
            UUID.fromString("11111111-2222-4333-8444-555555555555");

    private static ObjectNode valid() throws Exception {
        return (ObjectNode) JSON.readTree("""
            {
              "format": "family-points-export-v2",
              "workspaceId": 17,
              "sourceEpoch": 7,
              "categories": {
                "FAMILY_ROLES": [
                  {"id":11,"sourceWorkspaceId":17,"code":"PARENT","systemRole":true}
                ],
                "FAMILY_MEMBERS": [
                  {"id":21,"sourceWorkspaceId":17,"sourceRoleId":11,
                   "memberType":"PARENT","activeAtSnapshot":true,
                   "deletedAtSnapshot":null}
                ],
                "POINT_TYPES": [
                  {"id":31,"family_id":17}
                ],
                "TASK_DEFINITIONS": [
                  {"id":41,"family_id":17,"reward_point_type_id":31,
                   "penalty_point_type_id":null,
                   "assigned_member_id":null,"created_by_member_id":null,
                   "preferred_member_id":null,"responsible_member_id":null}
                ],
                "REWARD_DEFINITIONS": [
                  {"id":51,"family_id":17,"point_type_id":31}
                ],
                "TASK_EXECUTIONS": [],
                "REWARD_PURCHASES": [],
                "POINT_LEDGER": []
              },
              "manifestRootDigest": ""
            }
            """);
    }

    private static String compute(ObjectNode root) {
        return PortableMemberAncestryV2.computeRootWithoutTrust(
                root, SOURCE_WORKSPACE, SNAPSHOT, SOURCE_EPOCH);
    }

    private static void assertRejected(ObjectNode root, String why) {
        assertThrows(SecurityException.class, () -> compute(root), why);
    }

    @Test
    void directFamilyCategoriesRejectOmittedNullAndWrongWorkspaceId() throws Exception {
        ObjectNode baseline = valid();
        String root = compute(baseline);
        baseline.put("manifestRootDigest", root);
        assertDoesNotThrow(() -> PortableMemberAncestryV2.verify(
                baseline, SOURCE_WORKSPACE, SNAPSHOT, SOURCE_EPOCH));

        for (String category : new String[]{
                "POINT_TYPES", "TASK_DEFINITIONS", "REWARD_DEFINITIONS"}) {
            ObjectNode missing = valid();
            ((ObjectNode) missing.path("categories").path(category).get(0))
                    .remove("family_id");
            assertRejected(missing, category + " omitted family_id");

            ObjectNode nullTenant = valid();
            ((ObjectNode) nullTenant.path("categories").path(category).get(0))
                    .putNull("family_id");
            assertRejected(nullTenant, category + " null family_id");

            ObjectNode wrongTenant = valid();
            ((ObjectNode) wrongTenant.path("categories").path(category).get(0))
                    .put("family_id", SOURCE_WORKSPACE + 1);
            assertRejected(wrongTenant, category + " foreign family_id");

            ObjectNode stringTenant = valid();
            ((ObjectNode) stringTenant.path("categories").path(category).get(0))
                    .put("family_id", String.valueOf(SOURCE_WORKSPACE));
            assertRejected(stringTenant, category + " string family_id");
        }
    }
}
