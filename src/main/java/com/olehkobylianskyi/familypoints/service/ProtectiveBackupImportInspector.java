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
        "REWARD_DEFINITIONS","REWARD_REQUESTS","REWARD_PURCHASES","MEMBER_GROUPS",
        "WORK_NODES","MEDIA_ASSETS","MEDIA_BINDINGS","POINT_LEDGER","REPUTATION_LEDGER");
    private final ProtectiveBackupPortableFile files;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper json=new ObjectMapper();
    public ProtectiveBackupImportInspector(ProtectiveBackupPortableFile files,
            ProtectiveBackupArtifactCodec codec) {this.files=files;this.codec=codec;}

    public record Inspection(UUID snapshotId,long workspaceId,long sourceEpoch,
            String schemaVersion,List<String> availableCategories,
            Map<String,Integer> counts,boolean restorable,List<String> blockers) {}

    private static void checkParent(JsonNode categories,Map<String,Set<Long>> ids,
            Set<String> blockers,String child,String column,String parent) {
        if(!categories.has(child))return;
        for(JsonNode row:categories.path(child)) {
            if(!row.path(column).canConvertToLong() || row.path(column).asLong()<=0) {
                blockers.add("INVALID_REFERENCE:"+child+"."+column);
            } else if(!categories.has(parent)) {
                blockers.add("MISSING_CATEGORY:"+parent);
            } else if(!ids.getOrDefault(parent,Set.of()).contains(row.path(column).asLong())) {
                blockers.add("DANGLING_REFERENCE:"+child+"."+column);
            }
        }
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
                if(entry.getValue().size()>100000)
                    throw new IllegalArgumentException("Archive category exceeds safety limit");
                var ids=new HashSet<String>();
                for(JsonNode item:entry.getValue()) {
                    if(!item.isObject())
                        throw new IllegalArgumentException("Archive row must be an object");
                    String rowKey;
                    if("MEDIA_BINDINGS".equals(name)) {
                        if(!item.path("workspace_id").canConvertToLong()
                                || !item.path("target_id").canConvertToLong()
                                || !item.path("target_type").isTextual()
                                || !item.path("slot").isTextual())
                            throw new IllegalArgumentException("Invalid media binding composite key");
                        rowKey=item.path("workspace_id").asText()+":"+
                                item.path("target_type").asText()+":"+
                                item.path("target_id").asText()+":"+item.path("slot").asText();
                    } else {
                        if(!item.path("id").canConvertToLong() || item.path("id").asLong()<=0)
                            throw new IllegalArgumentException("Archive row requires positive numeric id");
                        rowKey=item.path("id").asText();
                    }
                    if(!ids.add(rowKey))
                        throw new IllegalArgumentException("Duplicate archive row key in "+name);
                    // Validate direct tenant keys. For FK-scoped descendants
                    // ownership must later be proven through their parent graph.
                    String ownerColumn="PRIVATE_TEMPLATES".equals(name)
                            || "MEDIA_BINDINGS".equals(name)?"workspace_id":
                            java.util.Set.of("MEMBER_GROUPS","TASK_DEFINITIONS",
                                    "REWARD_DEFINITIONS","REWARD_REQUESTS","WORK_NODES")
                                    .contains(name)?"family_id":null;
                    if(ownerColumn!=null && (!item.path(ownerColumn).canConvertToLong()
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
                if("MEDIA_BINDINGS".equals(cat.getKey()))continue;
                var idsForCategory=new HashSet<Long>();
                for(JsonNode row:cat.getValue()) {
                    if(row.path("id").canConvertToLong())idsForCategory.add(row.path("id").asLong());
                }
                rowIds.put(cat.getKey(),idsForCategory);
            }
            checkParent(categories,rowIds,blockers,"TASK_SCHEDULE_SLOTS","task_definition_id","TASK_DEFINITIONS");
            for(JsonNode binding:categories.path("MEDIA_BINDINGS")) {
                if(binding.hasNonNull("asset_id")) {
                    if(!categories.has("MEDIA_ASSETS")) {
                        blockers.add("MISSING_CATEGORY:MEDIA_ASSETS");
                    } else {
                        // Media asset identifiers are UUIDs, not numeric IDs.
                        boolean found=false;
                        for(JsonNode asset:categories.path("MEDIA_ASSETS"))
                            if(binding.path("asset_id").asText().equals(asset.path("id").asText()))
                                found=true;
                        if(!found)blockers.add("DANGLING_REFERENCE:MEDIA_BINDINGS.asset_id");
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
