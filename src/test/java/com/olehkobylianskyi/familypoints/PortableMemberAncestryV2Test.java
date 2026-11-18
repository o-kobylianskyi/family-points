package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.PortableManifestRootV2;
import com.olehkobylianskyi.familypoints.service.PortableMemberAncestryV2;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PortableMemberAncestryV2Test {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final UUID SNAP=
            UUID.fromString("11111111-2222-4333-8444-555555555555");
    private static ObjectNode source() throws Exception {
        return (ObjectNode)JSON.readTree("""
                {
                  "format":"family-points-export-v2",
                  "workspaceId":17,
                  "sourceEpoch":7,
                  "categories":{
                    "FAMILY_ROLES":[
                      {"id":11,"sourceWorkspaceId":17,"code":"PARENT","systemRole":true}
                    ],
                    "FAMILY_MEMBERS":[
                      {"id":21,"sourceWorkspaceId":17,"sourceRoleId":11,
                       "memberType":"CHILD","activeAtSnapshot":false,
                       "deletedAtSnapshot":"2026-11-01T08:09:10"}
                    ]
                  },
                  "manifestRootDigest":""
                }
                """);
    }
    @Test void actualCategoryJsonAndDependencyVectorsMatchIndependentPython() throws Exception {
        var source=source();
        var root=PortableMemberAncestryV2.computeRootWithoutTrust(
                source,17,SNAP,7);
        assertEquals("dcda588e22db2d804f2b68e4de43bf5e3051a45a7bd68170f673c29dce7e87c1",
                root);
        // Root computation is intentionally non-mutating. The authenticated
        // payload must explicitly carry the independently computed digest.
        assertEquals("",source.path("manifestRootDigest").asText());
        source.put("manifestRootDigest",root);
        var result=PortableMemberAncestryV2.verify(source,17,SNAP,7);
        assertEquals("0eb20ca0b2e56292e3a879d0c04513322f342be08f1ed14d13bc2b4dfafe81bb",
                result.categories().get("FAMILY_ROLES").contentSha256());
        assertEquals("321a6081a6f7dbd7645bd1d7f25bcb13bbb6a8a8e8dd8357c2a9625b0dd3f041",
                result.categories().get("FAMILY_MEMBERS").dependencyClosureSha256());
        assertEquals(root,result.manifestRootDigest());
    }
    @Test void originalMemberAndRoleCrossFamilyOrOmittedTombstoneDenied()
            throws Exception {
        var src=source();
        PortableMemberAncestryV2.computeRootWithoutTrust(src,17,SNAP,7);
        var role=((com.fasterxml.jackson.databind.node.ObjectNode)
                src.path("categories").path("FAMILY_ROLES").get(0));
        role.put("sourceWorkspaceId",18);
        var invalidRoleSource=src;
        assertThrows(SecurityException.class,()->
                PortableMemberAncestryV2.verify(invalidRoleSource,17,SNAP,7));
        src=source();
        var row=((com.fasterxml.jackson.databind.node.ObjectNode)
                src.path("categories").path("FAMILY_MEMBERS").get(0));
        row.remove("deletedAtSnapshot");
        var invalid=src;
        assertThrows(SecurityException.class,()->
                PortableMemberAncestryV2.verify(invalid,17,SNAP,7));
    }
}
