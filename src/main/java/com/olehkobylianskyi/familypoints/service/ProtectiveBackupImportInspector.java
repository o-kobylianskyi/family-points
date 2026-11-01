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
            // Parse/cryptographic validation does not prove FK closure, portable
            // identities, restored media or transactional recovery.
            return new Inspection(manifest.snapshotId(),targetWorkspace,manifest.dataEpoch(),
                    manifest.schemaVersion(),List.copyOf(names),Map.copyOf(counts),false,
                    List.of("RESTORE_ENGINE_NOT_IMPLEMENTED","FK_AND_IDENTITY_MAPPING_NOT_APPROVED",
                            "TARGET_CONFLICT_PREVIEW_REQUIRED","PROTECTIVE_TARGET_SNAPSHOT_REQUIRED"));
        } catch(java.io.IOException e) {
            throw new IllegalArgumentException("Malformed decrypted family archive",e);
        } finally {
            Arrays.fill(plaintext,(byte)0);
        }
    }
}
