package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * IMP-01 minimal versioned original-member source ancestry. Strictly supports
 * FAMILY_ROLES + FAMILY_MEMBERS; does not claim selected task/finance closure,
 * owner authentication, RESTORE authorization or a full restorable archive.
 *
 * Canonical category bytes: UTF-8 JSON, objects recursively key-sorted,
 * array rows sorted by source ID with explicit nulls. Dependency bytes:
 * UTF-8 domain-prefixed, length-prefixed sorted parent pairs for MEMBERS;
 * no parent pairs for ROLES. All data is later sealed by the FPB1 AES-GCM
 * codec. No external input can supply an unverified root digest.
 */
public final class PortableMemberAncestryV2 {
    private static final String CLOSURE_DOMAIN="family-points:portable:member-deps:v2";
    private static final ObjectMapper JSON=new ObjectMapper();
    public record Proof(String manifestRootDigest,String sourceDigest,
                        Map<String,PortableManifestRootV2.Category> categories) {}
    private PortableMemberAncestryV2() {}

    public static Proof verify(ObjectNode root,long sourceWorkspaceId,UUID snapshotId,
                               long sourceEpoch) {
        if(root==null||root.size()!=5
                ||!PortableManifestRootV2.SCHEMA.equals(root.path("format").asText())
                ||!root.path("workspaceId").isIntegralNumber()
                ||root.path("workspaceId").asLong()!=sourceWorkspaceId
                ||!root.path("sourceEpoch").isIntegralNumber()
                ||root.path("sourceEpoch").asLong()!=sourceEpoch)
            throw new SecurityException("PORTABLE_V2_SOURCE_SCOPE_INVALID");
        JsonNode source=root.path("categories");
        if(!source.isObject()||(source.size()!=2&&source.size()!=8)
                ||!source.path("FAMILY_ROLES").isArray()
                ||!source.path("FAMILY_MEMBERS").isArray())
            throw new SecurityException("PORTABLE_V2_ROSTER_CATEGORIES_REQUIRED");
        var roles=source.path("FAMILY_ROLES");
        var members=source.path("FAMILY_MEMBERS");
        if(roles.size()>2000||members.size()>2000)
            throw new SecurityException("PORTABLE_V2_ROSTER_ROW_LIMIT");
        var roleIds=new HashSet<Long>();
        var memberIds=new HashSet<Long>();
        var roleCodes=new HashSet<String>();
        var dependencies=new ArrayList<String>();
        long last=0;
        for(JsonNode role:roles) {
            long id=positive(role,"id");
            if(id<=last||!roleIds.add(id)||positive(role,"sourceWorkspaceId")!=sourceWorkspaceId
                    ||!role.path("code").isTextual()
                    ||!role.path("code").asText().matches("[A-Z][A-Z0-9_]{0,63}")
                    ||!roleCodes.add(role.path("code").asText())
                    ||!role.path("systemRole").isBoolean()
                    ||role.size()!=4)
                throw new SecurityException("PORTABLE_V2_ROLE_INVALID");
            last=id;
        }
        last=0;
        for(JsonNode member:members) {
            long id=positive(member,"id");
            long role=positive(member,"sourceRoleId");
            if(id<=last||!memberIds.add(id)||!roleIds.contains(role)
                    ||positive(member,"sourceWorkspaceId")!=sourceWorkspaceId
                    ||!Set.of("PARENT","CHILD","OTHER")
                            .contains(member.path("memberType").asText())
                    ||!member.path("activeAtSnapshot").isBoolean()
                    ||!member.has("deletedAtSnapshot")
                    ||member.hasNonNull("deletedAtSnapshot")
                        &&(!member.path("deletedAtSnapshot").isTextual()
                        ||!validTime(member.path("deletedAtSnapshot").asText()))
                    ||member.size()!=6)
                throw new SecurityException("PORTABLE_V2_MEMBER_INVALID");
            last=id;
            dependencies.add(id+":"+role);
        }
        var manifest=new TreeMap<String,PortableManifestRootV2.Category>();
        manifest.put("FAMILY_ROLES",new PortableManifestRootV2.Category(
                roles.size(),hashCanonical(roles),hashClosure("FAMILY_ROLES",List.of())));
        manifest.put("FAMILY_MEMBERS",new PortableManifestRootV2.Category(
                members.size(),hashCanonical(members),
                hashClosure("FAMILY_MEMBERS",dependencies)));
        if(source.size()==8) {
            var checked=PortableV2HistoryGraph.attest((ObjectNode)source,sourceWorkspaceId);
            manifest.putAll(checked);
        }
        String authenticatedRoot=PortableManifestRootV2.digest(
                sourceWorkspaceId,snapshotId,sourceEpoch,manifest);
        if(!root.path("manifestRootDigest").isTextual()
                ||!constantEquals(authenticatedRoot,root.path("manifestRootDigest").asText()))
            throw new SecurityException("PORTABLE_V2_ROOT_MISMATCH");
        String sourceDigest=hash(("family-points:portable:source-roster:v2|"
                +authenticatedRoot).getBytes(StandardCharsets.UTF_8));
        return new Proof(authenticatedRoot,sourceDigest,Map.copyOf(manifest));
    }

    public static String computeRootWithoutTrust(ObjectNode root,long sourceWorkspace,
                                         UUID snapshot,long epoch) {
        ObjectNode candidate=root.deepCopy();
        candidate.put("manifestRootDigest","0".repeat(64));
        // Repeat source graph validation but bypass only root comparison by
        // calculating from canonical independently validated category rows.
        return rawRoot(candidate,sourceWorkspace,snapshot,epoch);
    }
    private static String rawRoot(ObjectNode root,long workspace,UUID snapshot,long epoch) {
        // Implementation delegates actual validation through a local two-pass
        // root: temporarily set the candidate root after independent hashing.
        JsonNode categories=root.path("categories");
        if(!categories.isObject()||(categories.size()!=2&&categories.size()!=8))
            throw new SecurityException("PORTABLE_V2_ROSTER_CATEGORIES_REQUIRED");
        JsonNode roles=categories.path("FAMILY_ROLES"),members=categories.path("FAMILY_MEMBERS");
        if(!roles.isArray()||!members.isArray())
            throw new SecurityException("PORTABLE_V2_ROSTER_CATEGORIES_REQUIRED");
        var deps=new ArrayList<String>();
        for(JsonNode row:members)
            deps.add(positive(row,"id")+":"+positive(row,"sourceRoleId"));
        var hashes=Map.of(
                "FAMILY_ROLES",new PortableManifestRootV2.Category(
                        roles.size(),hashCanonical(roles),hashClosure("FAMILY_ROLES",List.of())),
                "FAMILY_MEMBERS",new PortableManifestRootV2.Category(
                        members.size(),hashCanonical(members),hashClosure("FAMILY_MEMBERS",deps)));
        var all=new TreeMap<String,PortableManifestRootV2.Category>(hashes);
        if(categories.size()==8)
            all.putAll(PortableV2HistoryGraph.attest((ObjectNode)categories,workspace));
        String digest=PortableManifestRootV2.digest(workspace,snapshot,epoch,all);
        root.put("manifestRootDigest",digest);
        verify(root,workspace,snapshot,epoch);
        return digest;
    }
    private static boolean validTime(String value) {
        try {java.time.LocalDateTime.parse(value);return true;}
        catch(java.time.format.DateTimeParseException invalid){return false;}
    }
    private static long positive(JsonNode row,String name) {
        JsonNode value=row.path(name);
        if(!value.isIntegralNumber()||value.asLong()<=0)
            throw new SecurityException("PORTABLE_V2_SOURCE_ID_INVALID:"+name);
        return value.asLong();
    }
    static String canonicalCategoryDigest(JsonNode rows) {
        return hashCanonical(rows);
    }
    private static String hashCanonical(JsonNode rows) {
        try {return hash(JSON.writeValueAsBytes(sorted(rows)));}
        catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new SecurityException("PORTABLE_V2_JSON_INVALID",e);
        }
    }
    private static JsonNode sorted(JsonNode node) {
        if(node.isObject()) {
            var result=JSON.createObjectNode();
            var fields=new TreeMap<String,JsonNode>();
            node.fields().forEachRemaining(e->fields.put(e.getKey(),e.getValue()));
            fields.forEach((name,value)->result.set(name,sorted(value)));
            return result;
        }
        if(node.isArray()) {
            var result=JSON.createArrayNode();
            for(JsonNode item:node)result.add(sorted(item));
            return result;
        }
        return node;
    }
    private static String hashClosure(String category,List<String> pairs) {
        // The first IMP-01 source slice has only one reviewed cross-category
        // parent: member.sourceRoleId -> familyRole.id. No other FK is claimed.
        var items=new ArrayList<>(pairs);
        Collections.sort(items);
        String canonical=CLOSURE_DOMAIN+"|"+category+"|"+String.join("|",items);
        return hash(canonical.getBytes(StandardCharsets.UTF_8));
    }
    private static String hash(byte[] data) {
        return ProtectiveBackupArtifactCodec.sha(data);
    }
    private static boolean constantEquals(String left,String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }
}
