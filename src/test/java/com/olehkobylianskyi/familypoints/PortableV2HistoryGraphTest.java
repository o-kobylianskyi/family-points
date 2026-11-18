package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.PortableMemberAncestryV2;
import com.olehkobylianskyi.familypoints.service.PortableV2HistoryGraph;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Strict real v2 source schema+FK closure; no RESTORE authorization. */
class PortableV2HistoryGraphTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final UUID SNAP=UUID.fromString("11111111-2222-4333-8444-555555555555");

    private static ObjectNode source() throws Exception {
        return (ObjectNode)JSON.readTree("""
            {
              "format":"family-points-export-v2","workspaceId":17,"sourceEpoch":7,
              "categories":{
                "FAMILY_ROLES":[{"id":11,"sourceWorkspaceId":17,"code":"PARENT","systemRole":true}],
                "FAMILY_MEMBERS":[{"id":21,"sourceWorkspaceId":17,"sourceRoleId":11,
                  "memberType":"PARENT","activeAtSnapshot":true,"deletedAtSnapshot":null}],
                "POINT_TYPES":[{"id":31,"family_id":17}],
                "TASK_DEFINITIONS":[{"id":41,"family_id":17,
                  "reward_point_type_id":31,"penalty_point_type_id":null,
                  "assigned_member_id":null,"created_by_member_id":null,
                  "preferred_member_id":null,"responsible_member_id":null}],
                "REWARD_DEFINITIONS":[{"id":51,"family_id":17,"point_type_id":31}],
                "TASK_EXECUTIONS":[{"id":61,"member_id":21,"task_definition_id":41,
                  "original_assigned_member_id":null,"reward_point_type_id":31,
                  "penalty_point_type_id":null}],
                "REWARD_PURCHASES":[{"id":71,"member_id":21,
                  "reward_definition_id":51,"point_type_id":31,"reward_request_id":null}],
                "POINT_LEDGER":[
                  {"id":81,"member_id":21,"point_type_id":31,"amount":5,"source_type":"TASK","source_id":61},
                  {"id":82,"member_id":21,"point_type_id":31,"amount":-4,"source_type":"REWARD_PURCHASE","source_id":71},
                  {"id":83,"member_id":21,"point_type_id":31,"amount":2,"source_type":"MANUAL","source_id":null}
                ]
              },
              "manifestRootDigest":""
            }
            """);
    }
    private static void seal(ObjectNode source) {
        source.put("manifestRootDigest",
                PortableMemberAncestryV2.computeRootWithoutTrust(source,17,SNAP,7));
    }
    @Test void fullBatchedSourceGraphIsBoundToSameRealV2Manifest() throws Exception {
        var source=source();
        seal(source);
        var proof=PortableMemberAncestryV2.verify(source,17,SNAP,7);
        assertEquals(8,proof.categories().size());
        assertEquals(3,proof.categories().get("POINT_LEDGER").rows());
        var altered=source.deepCopy();
        ((ObjectNode)altered.path("categories").path("POINT_LEDGER").get(0))
                .put("source_id",71);
        assertThrows(SecurityException.class,()->
                PortableMemberAncestryV2.verify(altered,17,SNAP,7));
    }
    @Test void missingSourceParentAndCrossFamilyDenials() throws Exception {
        var source=source();
        ((ObjectNode)source.path("categories").path("TASK_EXECUTIONS").get(0))
                .put("task_definition_id",999L);
        var orphan=source;
        assertThrows(SecurityException.class,()->seal(orphan));
        source=source();
        ((ObjectNode)source.path("categories").path("POINT_TYPES").get(0))
                .put("family_id",18);
        var crossFamily=source;
        assertThrows(SecurityException.class,()->seal(crossFamily));
    }
    @Test void missingCategoryOrOmittedNullableFieldIsNotSilentlyRepaired()
            throws Exception {
        var source=source();
        ((ObjectNode)source.path("categories")).remove("POINT_TYPES");
        var missing=source;
        assertThrows(SecurityException.class,()->seal(missing));
        source=source();
        ((ObjectNode)source.path("categories").path("TASK_DEFINITIONS").get(0))
                .remove("penalty_point_type_id");
        var incomplete=source;
        assertThrows(SecurityException.class,()->seal(incomplete));
    }
    @Test void unsupportedFinancialEventOrUnexportedPurchaseRequestFailsClosed()
            throws Exception {
        var source=source();
        ((ObjectNode)source.path("categories").path("POINT_LEDGER").get(0))
                .put("source_type","REFUND");
        var refund=source;
        assertThrows(SecurityException.class,()->seal(refund));
        source=source();
        ((ObjectNode)source.path("categories").path("REWARD_PURCHASES").get(0))
                .put("reward_request_id",333);
        var request=source;
        assertThrows(SecurityException.class,()->seal(request));
    }
    @Test void emptyHistoricalGraphIsAllowedButAlwaysNonRestorable() throws Exception {
        var source=source();
        var cats=(ObjectNode)source.path("categories");
        for(String name:List.of("POINT_TYPES","TASK_DEFINITIONS",
                "REWARD_DEFINITIONS","TASK_EXECUTIONS","REWARD_PURCHASES","POINT_LEDGER"))
            cats.putArray(name);
        seal(source);
        var proof=PortableMemberAncestryV2.verify(source,17,SNAP,7);
        assertEquals(0,proof.categories().get("POINT_LEDGER").rows());
    }
}
