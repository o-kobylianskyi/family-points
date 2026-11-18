package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.*;

/**
 * Offline-only portable import preflight. No HTTP upload, database writes,
 * merge/restore or VERIFIED promotion. The decrypted archive is untrusted input.
 * Fail closed on unknown schema, extra categories, workspace mismatch, or
 * any malformed category data. No implicit cross-family migration.
 */
@Component
public final class ProtectiveBackupImportInspector {
    private static final Set<String> CATEGORIES=Set.of(
        "PRIVATE_TEMPLATES","TASK_DEFINITIONS","TASK_EXECUTIONS","TASK_SCHEDULE_SLOTS",
        "REWARD_DEFINITIONS","REWARD_REQUESTS","REWARD_REQUIREMENTS","REWARD_PURCHASES",
        "REWARD_CATEGORIES","REWARD_DEFINITION_CATEGORIES",
        "REWARD_OBLIGATIONS","REWARD_OBLIGATION_BLOCKED_CATEGORIES",
        "REWARD_OBLIGATION_BLOCKED_REWARDS","MEMBER_GROUPS",
        "WORK_NODES","MEDIA_ASSETS","MEDIA_BINDINGS","POINT_LEDGER","REPUTATION_LEDGER",
        "GROUP_POINT_LEDGER","POINT_TYPES",
        "POINT_EXCHANGE_RATES","POINT_EXCHANGE_REQUESTS");
    private final ProtectiveBackupPortableFile files;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper json=new ObjectMapper();
    public ProtectiveBackupImportInspector(ProtectiveBackupPortableFile files,
            ProtectiveBackupArtifactCodec codec) {this.files=files;this.codec=codec;}

    public record Inspection(UUID snapshotId,long workspaceId,long sourceEpoch,
            String schemaVersion,List<String> availableCategories,
            Map<String,Integer> counts,boolean restorable,List<String> blockers) {}

    /**
     * IMP-01a: offline authenticated verification of the minimal v2 original
     * member/role ancestry source slice. NO database, writer, owner proof or
     * restore permission; the existing v1 inspect method is unchanged.
     */
    public record AncestryV2Inspection(UUID snapshotId,long sourceWorkspaceId,
            long sourceEpoch,String manifestRootDigest,int roleCount,int memberCount,
            List<Long> sourceRoleIds,List<Long> sourceMemberIds,boolean restorable) {
        public AncestryV2Inspection {
            sourceRoleIds=List.copyOf(sourceRoleIds);
            sourceMemberIds=List.copyOf(sourceMemberIds);
        }
    }
    public AncestryV2Inspection inspectAncestryV2(byte[] file,long sourceWorkspaceId,
                                                  SecretKey key) {
        if(sourceWorkspaceId<=0)
            throw new SecurityException("PORTABLE_V2_SOURCE_SCOPE_INVALID");
        var artifact=files.decode(file);
        var manifest=artifact.manifest();
        if(manifest.workspaceId()!=sourceWorkspaceId
                ||!PortableManifestRootV2.SCHEMA.equals(manifest.schemaVersion()))
            throw new SecurityException("PORTABLE_V2_ARCHIVE_SCOPE_OR_VERSION");
        byte[] plaintext=codec.open(artifact,manifest.snapshotId(),sourceWorkspaceId,key);
        try {
            JsonNode parsed=json.readTree(plaintext);
            if(!(parsed instanceof com.fasterxml.jackson.databind.node.ObjectNode root))
                throw new SecurityException("PORTABLE_V2_ROOT_OBJECT_REQUIRED");
            var proof=PortableMemberAncestryV2.verify(root,sourceWorkspaceId,
                    manifest.snapshotId(),manifest.dataEpoch());
            if(!java.security.MessageDigest.isEqual(
                    proof.sourceDigest().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    manifest.sourceDigest().getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                throw new SecurityException("PORTABLE_V2_SOURCE_DIGEST_MISMATCH");
            // Expose verified historic IDs for subsequent mapping review,
            // never archive account identities or mint target-member roles.
            // PortableMemberAncestryV2.verify has already enforced sorted,
            // unique, positive IDs and role-family closure.
            var roles=new ArrayList<Long>();
            var members=new ArrayList<Long>();
            for(JsonNode row:root.path("categories").path("FAMILY_ROLES"))
                roles.add(row.path("id").asLong());
            for(JsonNode row:root.path("categories").path("FAMILY_MEMBERS"))
                members.add(row.path("id").asLong());
            return new AncestryV2Inspection(manifest.snapshotId(),sourceWorkspaceId,
                    manifest.dataEpoch(),proof.manifestRootDigest(),
                    roles.size(),members.size(),roles,members,false);
        }catch(java.io.IOException invalid) {
            throw new SecurityException("PORTABLE_V2_SOURCE_JSON_INVALID",invalid);
        }finally {
            Arrays.fill(plaintext,(byte)0);
        }
    }

    private static void checkParent(JsonNode categories,Map<String,Set<Long>> ids,
            Set<String> blockers,String child,String column,String parent) {
        if(!categories.has(child))return;
        for(JsonNode row:categories.path(child)) {
            if(!row.path(column).isIntegralNumber() || row.path(column).asLong()<=0) {
                blockers.add("INVALID_REFERENCE:"+child+"."+column);
            } else if(!categories.has(parent)) {
                blockers.add("MISSING_CATEGORY:"+parent);
            } else if(!ids.getOrDefault(parent,Set.of()).contains(row.path(column).asLong())) {
                blockers.add("DANGLING_REFERENCE:"+child+"."+column);
            }
        }
    }

    private static void checkOptionalParent(JsonNode categories,Map<String,Set<Long>> ids,
            Set<String> blockers,String child,String column,String parent) {
        if(!categories.has(child))return;
        for(JsonNode row:categories.path(child)) {
            JsonNode reference=row.path(column);
            if(reference.isMissingNode() || reference.isNull())continue;
            if(!reference.isIntegralNumber() || reference.asLong()<=0)
                blockers.add("INVALID_REFERENCE:"+child+"."+column);
            else if(!categories.has(parent))
                blockers.add("MISSING_CATEGORY:"+parent);
            else if(!ids.getOrDefault(parent,Set.of()).contains(reference.asLong()))
                blockers.add("DANGLING_REFERENCE:"+child+"."+column);
        }
    }

    private static void checkMemberParent(JsonNode categories,Set<String> blockers,String child) {
        if(!categories.has(child))return;
        for(JsonNode row:categories.path(child)) {
            if(!row.path("member_id").isIntegralNumber() || row.path("member_id").asLong()<=0)
                blockers.add("INVALID_REFERENCE:"+child+".member_id");
            // FamilyMembers are identity-protected and intentionally omitted
            // from this archive version. Cross-family identity resolution is
            // therefore unverified even when the foreign ID is plausible.
            else blockers.add("IDENTITY_REBIND_REQUIRED:"+child+".member_id");
        }
    }

    private static boolean isUuid(String value) {
        try {return UUID.fromString(value).toString().equalsIgnoreCase(value);}
        catch(RuntimeException ex) {return false;}
    }

    public Inspection inspect(byte[] bytes,long targetWorkspace,SecretKey key) {
        var artifact=files.decode(bytes);
        var manifest=artifact.manifest();
        if(targetWorkspace<=0 || manifest.workspaceId()!=targetWorkspace)
            throw new SecurityException("BACKUP_WRONG_WORKSPACE");
        byte[] plaintext=codec.open(artifact,manifest.snapshotId(),targetWorkspace,key);
        try {
            JsonNode root=json.readTree(plaintext);
            if(root==null || !root.isObject() || root.size()!=3
                    || !root.path("format").isTextual()
                    || !"family-points-export-v1".equals(root.path("format").asText())
                    || !root.path("workspaceId").canConvertToLong()
                    || root.path("workspaceId").asLong()!=targetWorkspace
                    || !root.path("categories").isObject())
                throw new IllegalArgumentException("Unsupported family archive schema");
            var categories=root.path("categories");
            var names=new TreeSet<String>();var counts=new TreeMap<String,Integer>();
            var iterator=categories.fields();
            while(iterator.hasNext()) {
                var entry=iterator.next();String name=entry.getKey();
                if(!CATEGORIES.contains(name) || !entry.getValue().isArray())
                    throw new IllegalArgumentException("Unknown or malformed archive category");
                if(entry.getValue().size()>2000)
                    throw new IllegalArgumentException("Archive category exceeds safety limit");
                var ids=new HashSet<String>();
                for(JsonNode item:entry.getValue()) {
                    if(!item.isObject())
                        throw new IllegalArgumentException("Archive row must be an object");
                    String rowKey;
                    if("MEDIA_BINDINGS".equals(name)) {
                        if(!item.path("workspace_id").isIntegralNumber()
                                || !item.path("target_id").isIntegralNumber()
                                || item.path("target_id").asLong()<=0
                                || !item.path("target_type").isTextual()
                                || !Set.of("WORKSPACE","MEMBER","MEMBER_GROUP","WORK_NODE","TASK")
                                        .contains(item.path("target_type").asText())
                                || !item.path("slot").isTextual()
                                || item.path("slot").asText().isBlank()
                                || ("WORKSPACE".equals(item.path("target_type").asText())
                                        && item.path("target_id").asLong()!=targetWorkspace)
                                || (item.hasNonNull("asset_id") && !isUuid(item.path("asset_id").asText())))
                            throw new IllegalArgumentException("Invalid media binding composite key");
                        rowKey=Long.toString(item.path("workspace_id").asLong())+":"+
                                item.path("target_type").asText()+":"+
                                item.path("target_id").asLong()+":"+item.path("slot").asText();
                    } else if("REWARD_OBLIGATION_BLOCKED_CATEGORIES".equals(name)
                            || "REWARD_OBLIGATION_BLOCKED_REWARDS".equals(name)) {
                        String target="REWARD_OBLIGATION_BLOCKED_CATEGORIES".equals(name)
                                ?"category_id":"reward_definition_id";
                        JsonNode obligation=item.path("obligation_id");
                        JsonNode parent=item.path(target);
                        if(!obligation.isIntegralNumber() || obligation.asLong()<=0
                                || !parent.isIntegralNumber() || parent.asLong()<=0)
                            throw new IllegalArgumentException("Invalid reward obligation block composite PK");
                        rowKey=obligation.asLong()+":"+parent.asLong();
                    } else if("REWARD_DEFINITION_CATEGORIES".equals(name)) {
                        JsonNode definition=item.path("reward_definition_id");
                        JsonNode category=item.path("category_id");
                        if(!definition.isIntegralNumber() || definition.asLong()<=0
                                || !category.isIntegralNumber() || category.asLong()<=0)
                            throw new IllegalArgumentException("Invalid reward-category association PK");
                        rowKey=definition.asLong()+":"+category.asLong();
                    } else if("MEDIA_ASSETS".equals(name)) {
                        try {
                            String id=item.path("id").asText();
                            if(!isUuid(id))throw new IllegalArgumentException("Noncanonical UUID");
                            rowKey=UUID.fromString(id).toString();
                        }
                        catch(IllegalArgumentException ex) {
                            throw new IllegalArgumentException("Media asset requires UUID id",ex);
                        }
                    } else {
                        if(!item.path("id").isIntegralNumber() || item.path("id").asLong()<=0)
                            throw new IllegalArgumentException("Archive row requires positive numeric id");
                        rowKey=Long.toString(item.path("id").asLong());
                    }
                    if(!ids.add(rowKey))
                        throw new IllegalArgumentException("Duplicate archive row key in "+name);
                    if("MEDIA_ASSETS".equals(name)) {
                        JsonNode blobs=item.path("blobs");
                        if(!blobs.isArray() || blobs.size()>3)
                            throw new IllegalArgumentException("Missing or oversized media variants");
                        var variants=new HashSet<String>();
                        for(JsonNode blob:blobs) {
                            if(!blob.isObject() || !blob.path("variant").isTextual()
                                    || !Set.of("COVER","CARD","THUMB").contains(blob.path("variant").asText())
                                    || !variants.add(blob.path("variant").asText())
                                    || !blob.path("bytesBase64").isTextual())
                                throw new IllegalArgumentException("Invalid media variant");
                            String encoded=blob.path("bytesBase64").asText();
                            if(encoded.length()>8_000_000)
                                throw new IllegalArgumentException("Media variant exceeds limit");
                            try {
                                byte[] mediaBytes=Base64.getDecoder().decode(encoded);
                                if(mediaBytes.length==0 || mediaBytes.length>5_242_880)
                                    throw new IllegalArgumentException("Invalid media binary length");
                                Arrays.fill(mediaBytes,(byte)0);
                            }catch(IllegalArgumentException exception) {
                                throw new IllegalArgumentException("Invalid encoded media payload",exception);
                            }
                        }
                    }
                    // Validate direct tenant keys. For FK-scoped descendants
                    // ownership must later be proven through their parent graph.
                    String ownerColumn="PRIVATE_TEMPLATES".equals(name)
                            || "MEDIA_BINDINGS".equals(name) || "MEDIA_ASSETS".equals(name)?"workspace_id":
                            java.util.Set.of("MEMBER_GROUPS","TASK_DEFINITIONS",
                                    "REWARD_DEFINITIONS","REWARD_REQUESTS","REWARD_REQUIREMENTS",
                                    "REWARD_CATEGORIES","REWARD_OBLIGATIONS","POINT_TYPES",
                                    "POINT_EXCHANGE_RATES","WORK_NODES")
                                    .contains(name)?"family_id":null;
                    if(ownerColumn!=null && (!item.path(ownerColumn).isIntegralNumber()
                            || item.path(ownerColumn).asLong()!=targetWorkspace))
                        throw new SecurityException("Archive row references another workspace");
                }
                names.add(name);counts.put(name,entry.getValue().size());
            }
            // Validate references that can be proven wholly within this
            // archive, without consulting or modifying the target database.
            // An absent parent category is reported as a restore blocker, not
            // silently assumed to exist in a different family.
            var blockers=new TreeSet<String>();
            Map<String,Set<Long>> rowIds=new HashMap<>();
            for(var entry=categories.fields();entry.hasNext();) {
                var cat=entry.next();
                if("MEDIA_BINDINGS".equals(cat.getKey())
                        || "REWARD_DEFINITION_CATEGORIES".equals(cat.getKey())
                        || "REWARD_OBLIGATION_BLOCKED_CATEGORIES".equals(cat.getKey())
                        || "REWARD_OBLIGATION_BLOCKED_REWARDS".equals(cat.getKey()))continue;
                var idsForCategory=new HashSet<Long>();
                for(JsonNode row:cat.getValue()) {
                    if(row.path("id").isIntegralNumber())idsForCategory.add(row.path("id").asLong());
                }
                rowIds.put(cat.getKey(),idsForCategory);
            }
            checkParent(categories,rowIds,blockers,
                    "REWARD_DEFINITION_CATEGORIES","reward_definition_id","REWARD_DEFINITIONS");
            checkParent(categories,rowIds,blockers,
                    "REWARD_DEFINITION_CATEGORIES","category_id","REWARD_CATEGORIES");
            checkParent(categories,rowIds,blockers,
                    "REWARD_OBLIGATIONS","reward_purchase_id","REWARD_PURCHASES");
            checkOptionalParent(categories,rowIds,blockers,
                    "REWARD_OBLIGATIONS","requirement_id","REWARD_REQUIREMENTS");
            checkMemberParent(categories,blockers,"REWARD_OBLIGATIONS");
            checkParent(categories,rowIds,blockers,
                    "REWARD_OBLIGATION_BLOCKED_CATEGORIES","obligation_id","REWARD_OBLIGATIONS");
            checkParent(categories,rowIds,blockers,
                    "REWARD_OBLIGATION_BLOCKED_CATEGORIES","category_id","REWARD_CATEGORIES");
            checkParent(categories,rowIds,blockers,
                    "REWARD_OBLIGATION_BLOCKED_REWARDS","obligation_id","REWARD_OBLIGATIONS");
            checkParent(categories,rowIds,blockers,
                    "REWARD_OBLIGATION_BLOCKED_REWARDS","reward_definition_id","REWARD_DEFINITIONS");
            checkParent(categories,rowIds,blockers,"TASK_SCHEDULE_SLOTS","task_definition_id","TASK_DEFINITIONS");
            // Portable archives cannot assume that a parent omitted by the
            // selected subset will be present in the target workspace.
            checkParent(categories,rowIds,blockers,"TASK_EXECUTIONS","task_definition_id","TASK_DEFINITIONS");
            checkOptionalParent(categories,rowIds,blockers,"REWARD_REQUESTS","reward_definition_id","REWARD_DEFINITIONS");
            checkOptionalParent(categories,rowIds,blockers,"REWARD_PURCHASES","reward_definition_id","REWARD_DEFINITIONS");
            if(categories.has("REWARD_REQUIREMENTS")) {
                for(JsonNode row:categories.path("REWARD_REQUIREMENTS")) {
                    boolean byDefinition=row.hasNonNull("reward_definition_id");
                    boolean byRequest=row.hasNonNull("reward_request_id");
                    if(byDefinition==byRequest)
                        blockers.add("INVALID_REWARD_REQUIREMENT_OWNER");
                }
            }
            checkOptionalParent(categories,rowIds,blockers,
                    "REWARD_REQUIREMENTS","reward_definition_id","REWARD_DEFINITIONS");
            checkOptionalParent(categories,rowIds,blockers,
                    "REWARD_REQUIREMENTS","reward_request_id","REWARD_REQUESTS");
            checkOptionalParent(categories,rowIds,blockers,
                    "REWARD_REQUIREMENTS","task_definition_id","TASK_DEFINITIONS");
            checkMemberParent(categories,blockers,"TASK_EXECUTIONS");
            checkMemberParent(categories,blockers,"REWARD_PURCHASES");
            // Purchase history can be tied to either a reward definition or
            // a request. These references are not interchangeable, and every
            // historic price denomination must be explicitly rebound.
            if(categories.has("REWARD_PURCHASES")) {
                for(JsonNode row:categories.path("REWARD_PURCHASES")) {
                    var requestId=row.path("reward_request_id");
                    if(!requestId.isMissingNode() && !requestId.isNull()) {
                        if(!requestId.isIntegralNumber() || requestId.asLong()<=0)
                            blockers.add("INVALID_REFERENCE:REWARD_PURCHASES.reward_request_id");
                        else if(!categories.has("REWARD_REQUESTS"))
                            blockers.add("MISSING_CATEGORY:REWARD_REQUESTS");
                        else if(!rowIds.getOrDefault("REWARD_REQUESTS",Set.of())
                                .contains(requestId.asLong()))
                            blockers.add("DANGLING_REFERENCE:REWARD_PURCHASES.reward_request_id");
                    }
                }
            }
            for(String category:List.of("REWARD_PURCHASES","REWARD_REQUESTS")) {
                for(JsonNode row:categories.path(category)) {
                    if("REWARD_REQUESTS".equals(category)) {
                        var requestedBy=row.path("requested_by_member_id");
                        if(!requestedBy.isIntegralNumber() || requestedBy.asLong()<=0)
                            blockers.add("INVALID_REFERENCE:REWARD_REQUESTS.requested_by_member_id");
                        else
                            blockers.add("IDENTITY_REBIND_REQUIRED:REWARD_REQUESTS.requested_by_member_id");
                    }
                    String field="REWARD_PURCHASES".equals(category)
                            ?"point_type_id":"approved_point_type_id";
                    JsonNode pointType=row.path(field);
                    if(pointType.isMissingNode() || pointType.isNull()) {
                        if("REWARD_PURCHASES".equals(category))
                            blockers.add("INVALID_REFERENCE:"+category+"."+field);
                    } else if(!pointType.isIntegralNumber() || pointType.asLong()<=0) {
                        blockers.add("INVALID_REFERENCE:"+category+"."+field);
                    } else {
                        blockers.add("CURRENCY_REBIND_REQUIRED:"+category+"."+field);
                    }
                }
            }

            checkParent(categories,rowIds,blockers,"GROUP_POINT_LEDGER","group_id","MEMBER_GROUPS");
            if(categories.has("GROUP_POINT_LEDGER")) {
                for(JsonNode row:categories.path("GROUP_POINT_LEDGER")) {
                    if(!row.path("point_type_id").isIntegralNumber()
                            || row.path("point_type_id").asLong()<=0)
                        blockers.add("INVALID_REFERENCE:GROUP_POINT_LEDGER.point_type_id");
                    else blockers.add("CURRENCY_REBIND_REQUIRED:GROUP_POINT_LEDGER.point_type_id");
                }
            }
            // Historical ledger source_id is polymorphic, NOT a database FK.
            // A cryptographically valid archive does not prove that the task,
            // purchase, exchange or adjustment referenced by an operation can
            // be reconstructed in the target workspace. No guessing IDs.
            for(String category:List.of("POINT_LEDGER","GROUP_POINT_LEDGER")) {
                for(JsonNode row:categories.path(category)) {
                    var sourceType=row.path("source_type");
                    if(!sourceType.isTextual() || sourceType.asText().isBlank())
                        blockers.add("INVALID_REFERENCE:"+category+".source_type");
                    else if(sourceType.asText().length()>40)
                        blockers.add("INVALID_REFERENCE:"+category+".source_type");
                    else if(!"MANUAL".equals(sourceType.asText()))
                        blockers.add("HISTORICAL_SOURCE_REMAP_REQUIRED:"+category+":"+sourceType.asText());
                    var sourceId=row.path("source_id");
                    if(!sourceId.isMissingNode() && !sourceId.isNull()) {
                        if(!sourceId.isIntegralNumber() || sourceId.asLong()<=0)
                            blockers.add("INVALID_REFERENCE:"+category+".source_id");
                        else
                            blockers.add("HISTORICAL_SOURCE_ID_REMAP_REQUIRED:"+category);
                    }
                    if("POINT_LEDGER".equals(category)) {
                        var pointType=row.path("point_type_id");
                        if(!pointType.isIntegralNumber() || pointType.asLong()<=0)
                            blockers.add("INVALID_REFERENCE:POINT_LEDGER.point_type_id");
                        else
                            blockers.add("CURRENCY_REBIND_REQUIRED:POINT_LEDGER.point_type_id");
                    }
                }
            }
            // A source currency is only provable when its code and metadata
            // are included in the same authenticated envelope. Never infer
            // denomination identity from a raw numeric ID.
            if(categories.has("POINT_TYPES")) {
                var usedCodes=new HashSet<String>();
                for(JsonNode row:categories.path("POINT_TYPES")) {
                    JsonNode code=row.path("code");
                    if(!code.isTextual() || code.asText().isBlank()
                            || !usedCodes.add(code.asText()))
                        blockers.add("INVALID_POINT_TYPE_CODE");
                }
            }
            for(String financeCategory:List.of("POINT_LEDGER","GROUP_POINT_LEDGER",
                    "REWARD_PURCHASES","REWARD_DEFINITIONS","REWARD_REQUESTS")) {
                if(!categories.has(financeCategory))continue;
                String column="REWARD_REQUESTS".equals(financeCategory)
                        ?"approved_point_type_id":"point_type_id";
                for(JsonNode row:categories.path(financeCategory)) {
                    JsonNode ref=row.path(column);
                    if("REWARD_REQUESTS".equals(financeCategory)
                            && (ref.isMissingNode() || ref.isNull()))continue;
                    if(!ref.isIntegralNumber() || ref.asLong()<=0)
                        blockers.add("INVALID_REFERENCE:"+financeCategory+"."+column);
                    else if(!categories.has("POINT_TYPES"))
                        blockers.add("MISSING_CATEGORY:POINT_TYPES");
                    else if(!rowIds.getOrDefault("POINT_TYPES",Set.of()).contains(ref.asLong()))
                        blockers.add("DANGLING_REFERENCE:"+financeCategory+"."+column);
                }
            }
            checkParent(categories,rowIds,blockers,
                    "POINT_EXCHANGE_RATES","from_point_type_id","POINT_TYPES");
            checkParent(categories,rowIds,blockers,
                    "POINT_EXCHANGE_RATES","to_point_type_id","POINT_TYPES");
            checkParent(categories,rowIds,blockers,
                    "POINT_EXCHANGE_REQUESTS","exchange_rate_id","POINT_EXCHANGE_RATES");
            checkParent(categories,rowIds,blockers,
                    "POINT_EXCHANGE_REQUESTS","from_point_type_id","POINT_TYPES");
            checkParent(categories,rowIds,blockers,
                    "POINT_EXCHANGE_REQUESTS","to_point_type_id","POINT_TYPES");
            checkMemberParent(categories,blockers,"POINT_EXCHANGE_REQUESTS");
            if(categories.has("POINT_EXCHANGE_RATES")) {
                for(JsonNode rate:categories.path("POINT_EXCHANGE_RATES")) {
                    long from=rate.path("from_point_type_id").asLong();
                    long to=rate.path("to_point_type_id").asLong();
                    if(from<=0 || to<=0 || from==to
                            || !rate.path("from_amount").isIntegralNumber()
                            || rate.path("from_amount").asLong()<=0
                            || !rate.path("to_amount").isIntegralNumber()
                            || rate.path("to_amount").asLong()<=0)
                        blockers.add("INVALID_EXCHANGE_RATE");
                }
            }
            if(categories.has("POINT_EXCHANGE_REQUESTS")) {
                var rateById=new HashMap<Long,JsonNode>();
                for(JsonNode rate:categories.path("POINT_EXCHANGE_RATES"))
                    rateById.put(rate.path("id").asLong(),rate);
                for(JsonNode request:categories.path("POINT_EXCHANGE_REQUESTS")) {
                    var rate=rateById.get(request.path("exchange_rate_id").asLong());
                    if(!request.path("status").isTextual()
                            || !Set.of("PENDING","APPROVED","REJECTED","CANCELLED")
                                .contains(request.path("status").asText())
                            || !request.path("from_amount").isIntegralNumber()
                            || request.path("from_amount").asLong()<=0
                            || !request.path("to_amount").isIntegralNumber()
                            || request.path("to_amount").asLong()<=0
                            || !request.path("rate_from_amount").isIntegralNumber()
                            || request.path("rate_from_amount").asLong()<=0
                            || !request.path("rate_to_amount").isIntegralNumber()
                            || request.path("rate_to_amount").asLong()<=0)
                        blockers.add("INVALID_EXCHANGE_REQUEST");
                    // The approved exchange is valued at the immutable
                    // per-request ratio snapshot, not the current mutable rate.
                    long fromAmount=request.path("from_amount").asLong();
                    long toAmount=request.path("to_amount").asLong();
                    long rateFrom=request.path("rate_from_amount").asLong();
                    long rateTo=request.path("rate_to_amount").asLong();
                    if(fromAmount<=0 || toAmount<=0 || rateFrom<=0 || rateTo<=0
                            || fromAmount>Integer.MAX_VALUE
                            || toAmount>Integer.MAX_VALUE
                            || rateFrom>Integer.MAX_VALUE
                            || rateTo>Integer.MAX_VALUE
                            || fromAmount%rateFrom!=0
                            || (fromAmount/rateFrom)*rateTo!=toAmount)
                        blockers.add("EXCHANGE_REQUEST_HISTORIC_RATE_MISMATCH");
                    if(request.hasNonNull("processed_by_member_id")) {
                        var reviewer=request.path("processed_by_member_id");
                        if(!reviewer.isIntegralNumber() || reviewer.asLong()<=0)
                            blockers.add("INVALID_REFERENCE:POINT_EXCHANGE_REQUESTS.processed_by_member_id");
                        else blockers.add("IDENTITY_REBIND_REQUIRED:POINT_EXCHANGE_REQUESTS.processed_by_member_id");
                    }
                    if(rate!=null
                            && (rate.path("from_point_type_id").asLong()!=request.path("from_point_type_id").asLong()
                                || rate.path("to_point_type_id").asLong()!=request.path("to_point_type_id").asLong()))
                        blockers.add("EXCHANGE_REQUEST_RATE_CURRENCY_MISMATCH");
                    // Original request snapshots, NOT current mutable rate
                    // amounts, are the authoritative historical economic terms.
                }
            }
            if(categories.has("POINT_LEDGER")) {
                var pairs=new HashMap<Long,List<JsonNode>>();
                for(JsonNode operation:categories.path("POINT_LEDGER")) {
                    if(!"POINT_EXCHANGE".equals(operation.path("source_type").asText()))continue;
                    long id=operation.path("source_id").asLong();
                    if(!operation.path("source_id").isIntegralNumber() || id<=0)
                        blockers.add("INVALID_EXCHANGE_LEDGER_SOURCE_ID");
                    else pairs.computeIfAbsent(id,ignored->new ArrayList<>()).add(operation);
                }
                if(!pairs.isEmpty()) {
                    if(!categories.has("POINT_EXCHANGE_REQUESTS"))
                        blockers.add("MISSING_CATEGORY:POINT_EXCHANGE_REQUESTS");
                    else {
                        var requestById=new HashMap<Long,JsonNode>();
                        for(JsonNode request:categories.path("POINT_EXCHANGE_REQUESTS"))
                            requestById.put(request.path("id").asLong(),request);
                        for(var entry:pairs.entrySet()) {
                            var request=requestById.get(entry.getKey());
                            if(request==null) {
                                blockers.add("DANGLING_REFERENCE:POINT_LEDGER.source_id");
                                continue;
                            }
                            var legs=entry.getValue();
                            if(!"APPROVED".equals(request.path("status").asText()) || legs.size()!=2) {
                                blockers.add("INVALID_EXCHANGE_LEDGER_PAIR");
                                continue;
                            }
                            boolean spend=false,earn=false;
                            for(JsonNode leg:legs) {
                                long owner=leg.path("member_id").asLong();
                                long pointType=leg.path("point_type_id").asLong();
                                long amount=leg.path("amount").asLong();
                                String kind=leg.path("type").asText();
                                if(owner!=request.path("member_id").asLong()) {
                                    blockers.add("EXCHANGE_LEDGER_OWNER_MISMATCH");
                                    continue;
                                }
                                if(pointType==request.path("from_point_type_id").asLong()
                                        && amount==-request.path("from_amount").asLong()
                                        && "SPEND".equals(kind) && !spend)spend=true;
                                else if(pointType==request.path("to_point_type_id").asLong()
                                        && amount==request.path("to_amount").asLong()
                                        && "EARN".equals(kind) && !earn)earn=true;
                                else blockers.add("EXCHANGE_LEDGER_ECONOMICS_MISMATCH");
                            }
                            if(!spend || !earn)blockers.add("INVALID_EXCHANGE_LEDGER_PAIR");
                        }
                    }
                }
            }
            checkMemberParent(categories,blockers,"POINT_LEDGER");
            checkMemberParent(categories,blockers,"REPUTATION_LEDGER");

            // A media binding uses polymorphic target_type/target_id without a physical FK.
            // Validate the archived target graph before any future restore executor
            // can treat an archive as sufficient evidence for mapping.
            var assetIds=new HashSet<String>();
            for(JsonNode asset:categories.path("MEDIA_ASSETS"))
                assetIds.add(asset.path("id").asText());
            for(JsonNode binding:categories.path("MEDIA_BINDINGS")) {
                String type=binding.path("target_type").asText();
                long targetId=binding.path("target_id").asLong();
                String parent= switch(type) {
                    case "MEMBER_GROUP" -> "MEMBER_GROUPS";
                    case "WORK_NODE" -> "WORK_NODES";
                    case "TASK" -> "TASK_DEFINITIONS";
                    default -> null;
                };
                if("MEMBER".equals(type))
                    blockers.add("IDENTITY_REBIND_REQUIRED:MEDIA_BINDINGS.target_id");
                else if(parent!=null) {
                    if(!categories.has(parent))
                        blockers.add("MISSING_CATEGORY:"+parent);
                    else if(!rowIds.getOrDefault(parent,Set.of()).contains(targetId))
                        blockers.add("DANGLING_REFERENCE:MEDIA_BINDINGS.target_id");
                }
                // Each binding must point to exactly one representation.
                // Null/empty catalog keys are not valid alternatives to
                // private binary assets. Never silently guess binding type.
                boolean privateAsset=binding.hasNonNull("asset_id");
                boolean catalogKey=binding.path("catalog_key").isTextual()
                        && !binding.path("catalog_key").asText().isBlank();
                if(privateAsset==catalogKey)
                    blockers.add("INVALID_MEDIA_BINDING_SOURCE");
                if(privateAsset) {
                    if(!categories.has("MEDIA_ASSETS")) {
                        blockers.add("MISSING_CATEGORY:MEDIA_ASSETS");
                    } else {
                        // Media asset identifiers are UUIDs, not numeric IDs.
                        if(!assetIds.contains(binding.path("asset_id").asText()))
                            blockers.add("DANGLING_REFERENCE:MEDIA_BINDINGS.asset_id");
                    }
                }
            }
            // Parse/cryptographic validation does not prove FK closure, portable
            // identities, restored media or transactional recovery.
            blockers.addAll(List.of("RESTORE_ENGINE_NOT_IMPLEMENTED",
                    "FK_AND_IDENTITY_MAPPING_NOT_APPROVED",
                    "TARGET_CONFLICT_PREVIEW_REQUIRED","PROTECTIVE_TARGET_SNAPSHOT_REQUIRED"));
            return new Inspection(manifest.snapshotId(),targetWorkspace,manifest.dataEpoch(),
                    manifest.schemaVersion(),List.copyOf(names),Map.copyOf(counts),false,
                    List.copyOf(blockers));
        } catch(java.io.IOException e) {
            throw new IllegalArgumentException("Malformed decrypted family archive",e);
        } finally {
            Arrays.fill(plaintext,(byte)0);
        }
    }
}
